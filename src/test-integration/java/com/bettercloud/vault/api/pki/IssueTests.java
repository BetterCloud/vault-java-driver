package com.bettercloud.vault.api.pki;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.RestResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * TODO: Document
 */
public class IssueTests {

    @Before
    public void setup() throws VaultException {
        final String address = System.getProperty("VAULT_ADDR");
        final String appId = System.getProperty("VAULT_APP_ID");
        final String userId = System.getProperty("VAULT_USER_ID");
        final String rootToken = System.getProperty("VAULT_TOKEN");

        assertNotNull(address);
        assertNotNull(appId);
        assertNotNull(userId);
        assertNotNull(rootToken);

        final VaultConfig config = new VaultConfig(address, rootToken);
        final Vault vault = new Vault(config);

        final LogicalResponse response = vault.pki().deleteRole("issueTestRole");
        final RestResponse restResponse = response.getRestResponse();
        assertEquals(204, restResponse.getStatus());
    }

    @Test
    public void testIssueCredential() throws VaultException, InterruptedException {
        final String token = System.getProperty("VAULT_TOKEN");
//        final String token = authenticate();
        final String address = System.getProperty("VAULT_ADDR");
        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);

        // Create a role
        final LogicalResponse createRoleResponse = vault.pki().createOrUpdateRole("issueTestRole",
                new RoleOptions()
                        .allowedDomains(new ArrayList<String>(){{ add("myvault.com"); }})
                        .allowSubdomains(true)
                        .maxTtl("9h")
        );
        assertEquals(204, createRoleResponse.getRestResponse().getStatus());
        Thread.sleep(3000);

        // Issue cert
        final LogicalResponse issueResponse = vault.pki().issue("issueTestRole", "test.myvault.com", null, null, null, null);
        final Map<String, String> data = issueResponse.getData();
        assertNotNull(data.get("certificate"));
        assertNotNull(data.get("private_key"));
        assertNotNull(data.get("serial_number"));
        assertNotNull(data.get("private_key_type"));
        assertNotNull(data.get("issuing_ca"));
    }

}
