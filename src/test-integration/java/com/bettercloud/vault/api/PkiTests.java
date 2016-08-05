package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.pki.CredentialFormat;
import com.bettercloud.vault.api.pki.RoleOptions;
import com.bettercloud.vault.response.PkiResponse;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.*;

/**
 * <p>Integration tests for for operations on Vault's <code>/v1/pki/*</code> REST endpoints.</p>
 *
 * <p>These tests require a Vault server to be up and running.  See the setup notes in
 * "src/test-integration/README.md".</p>
 */
public class PkiTests {

    final static String appId = "fake_app";
    final static String userId = "fake_user";
    final static String password = "fake_password";
    final static String rootToken = "36303304-3f53-a0c9-af5d-3ffc8dabe683";

    @Rule
    public final VaultContainer container = new VaultContainer(rootToken);


    @Test
    public void testCreateRole_Defaults() throws VaultException, IOException, InterruptedException {
        container.createPkiExample();
        final String token = authenticate();
        final Vault vault = container.getVault(token);

        vault.pki().createOrUpdateRole("testRole");
        final PkiResponse response = vault.pki().getRole("testRole");
        assertTrue(compareRoleOptions(new RoleOptions(), response.getRoleOptions()));
    }

    @Test
    public void testCreateRole_WithOptions() throws VaultException, IOException, InterruptedException {
        container.createPkiExample();
        final String token = authenticate();
        final Vault vault = container.getVault(token);

        final RoleOptions options = new RoleOptions().allowAnyName(true);
        vault.pki().createOrUpdateRole("testRole", options);
        final PkiResponse response = vault.pki().getRole("testRole");
        assertTrue(compareRoleOptions(options, response.getRoleOptions()));
    }

    @Test
    public void testDeleteRole() throws VaultException, IOException, InterruptedException {
        container.createPkiExample();
        final String token = authenticate();
        final Vault vault = container.getVault(token);

        testCreateRole_Defaults();
        final PkiResponse deleteResponse = vault.pki().deleteRole("testRole");
        assertEquals(204, deleteResponse.getRestResponse().getStatus());
        final PkiResponse getResponse = vault.pki().getRole("testRole");
        assertEquals(404, getResponse.getRestResponse().getStatus());
    }

    @Test
    public void testIssueCredential() throws VaultException, InterruptedException, IOException {
        container.createPkiExample();
        final String token = authenticate();
        final Vault vault = container.getVault(token);

        // Create a role
        final PkiResponse createRoleResponse = vault.pki().createOrUpdateRole("testRole",
                new RoleOptions()
                        .allowedDomains(new ArrayList<String>(){{ add("myvault.com"); }})
                        .allowSubdomains(true)
                        .maxTtl("9h")
        );
        assertEquals(204, createRoleResponse.getRestResponse().getStatus());
        Thread.sleep(3000);

        // Issue cert
        final PkiResponse issueResponse = vault.pki().issue("testRole", "test.myvault.com", null, null, null, CredentialFormat.PEM);
        assertNotNull(issueResponse.getCredential().getCertificate());
        assertNotNull(issueResponse.getCredential().getPrivateKey());
        assertNotNull(issueResponse.getCredential().getSerialNumber());
        assertEquals("rsa", issueResponse.getCredential().getPrivateKeyType());
        assertNotNull(issueResponse.getCredential().getIssuingCa());
    }


    private String authenticate() throws VaultException, IOException, InterruptedException {
        container.createAuthExample(appId, userId, password);
        final String path = "userpass/login/" + userId;
        final Vault vault = container.getVault();

        final String token = vault.auth().loginByUsernamePassword(path, password).getAuthClientToken();
        assertNotNull(token);
        assertNotSame("", token.trim());
        return token;
    }

    private boolean compareRoleOptions(final RoleOptions expected, final RoleOptions actual) {
        if (expected.getAllowAnyName() != null && !expected.getAllowAnyName().equals(actual.getAllowAnyName())) return false;
        if (expected.getAllowBareDomains() != null && !expected.getAllowBareDomains().equals(actual.getAllowBareDomains())) return false;
        if (expected.getAllowedDomains() != null) {
            if (!expected.getAllowedDomains().containsAll(actual.getAllowedDomains())
                    || !actual.getAllowedDomains().containsAll(expected.getAllowedDomains())) {
                return false;
            }
        }
        if (expected.getAllowIpSans() != null && !expected.getAllowIpSans().equals(actual.getAllowIpSans())) return false;
        if (expected.getAllowLocalhost() != null && !expected.getAllowLocalhost().equals(actual.getAllowLocalhost())) return false;
        if (expected.getAllowSubdomains() != null && !expected.getAllowSubdomains().equals(actual.getAllowSubdomains())) return false;
        if (expected.getClientFlag() != null && !expected.getClientFlag().equals(actual.getClientFlag())) return false;
        if (expected.getCodeSigningFlag() != null && !expected.getCodeSigningFlag().equals(actual.getCodeSigningFlag())) return false;
        if (expected.getEmailProtectionFlag() != null && !expected.getEmailProtectionFlag().equals(actual.getEmailProtectionFlag())) return false;
        if (expected.getKeyBits() != null && !expected.getKeyBits().equals(actual.getKeyBits())) return false;
        if (expected.getKeyType() != null && !expected.getKeyType().equals(actual.getKeyType())) return false;
        if (expected.getMaxTtl() != null && !expected.getMaxTtl().equals(actual.getMaxTtl())) return false;
        if (expected.getServerFlag() != null && !expected.getServerFlag().equals(actual.getServerFlag())) return false;
        if (expected.getTtl() != null && !expected.getTtl().equals(actual.getTtl())) return false;
        if (expected.getUseCsrCommonName() != null && !expected.getUseCsrCommonName().equals(actual.getUseCsrCommonName())) return false;
        return true;
    }

}
