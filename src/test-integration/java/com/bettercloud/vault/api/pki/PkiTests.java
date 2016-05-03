package com.bettercloud.vault.api.pki;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.RestResponse;
import org.junit.Before;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public abstract class PkiTests {

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

//        final String token = authenticate();
        final VaultConfig config = new VaultConfig(address, rootToken);
        final Vault vault = new Vault(config);

        final LogicalResponse response = vault.pki().deleteRole("testRole");
        final RestResponse restResponse = response.getRestResponse();
        assertEquals(204, restResponse.getStatus());
    }


}
