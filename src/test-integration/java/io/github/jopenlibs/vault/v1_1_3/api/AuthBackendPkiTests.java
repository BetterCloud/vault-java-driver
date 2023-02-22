package io.github.jopenlibs.vault.v1_1_3.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.pki.CredentialFormat;
import io.github.jopenlibs.vault.api.pki.RoleOptions;
import io.github.jopenlibs.vault.response.PkiResponse;
import io.github.jopenlibs.vault.rest.RestResponse;
import io.github.jopenlibs.vault.v1_1_3.util.SSLUtils;
import io.github.jopenlibs.vault.v1_1_3.util.VaultContainer;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Integration tests for for operations on Vault's <code>/v1/pki/*</code> REST endpoints.
 */
public class AuthBackendPkiTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
        container.setupBackendPki();
    }

    @Before
    public void setup() throws VaultException {
        final Vault vault = container.getRootVault();

        final PkiResponse defaultResponse = vault.pki().deleteRole("testRole");
        final RestResponse defaultRestResponse = defaultResponse.getRestResponse();
        assertEquals(204, defaultRestResponse.getStatus());

        final PkiResponse customResponse = vault.pki("other-pki").deleteRole("testRole");
        final RestResponse customRestResponse = customResponse.getRestResponse();
        assertEquals(204, customRestResponse.getStatus());
    }

    @Test
    public void testCreateRole_Defaults() throws VaultException {
        final Vault vault = container.getRootVault();

        vault.pki().createOrUpdateRole("testRole");
        final PkiResponse response = vault.pki().getRole("testRole");
        assertTrue(compareRoleOptions(new RoleOptions(), response.getRoleOptions()));
    }

    @Test
    public void testCreateRole_WithOptions() throws VaultException {
        final Vault vault = container.getRootVault();

        final RoleOptions options = new RoleOptions().allowAnyName(true);
        vault.pki().createOrUpdateRole("testRole", options);
        final PkiResponse response = vault.pki().getRole("testRole");
        assertTrue(compareRoleOptions(options, response.getRoleOptions()));
    }

    @Test
    public void testDeleteRole() throws VaultException {
        final Vault vault = container.getRootVault();

        testCreateRole_Defaults();
        final PkiResponse deleteResponse = vault.pki().deleteRole("testRole");
        TestCase.assertEquals(204, deleteResponse.getRestResponse().getStatus());
        final PkiResponse getResponse = vault.pki().getRole("testRole");
        TestCase.assertEquals(404, getResponse.getRestResponse().getStatus());
    }

    @Test
    public void testIssueCredential() throws VaultException, InterruptedException {
        final Vault vault = container.getRootVault();

        // Create a role
        final PkiResponse createRoleResponse = vault.pki().createOrUpdateRole("testRole",
                new RoleOptions()
                        .allowedDomains(new ArrayList<String>() {{
                            add("myvault.com");
                        }})
                        .allowSubdomains(true)
                        .maxTtl("9h")
        );
        TestCase.assertEquals(204, createRoleResponse.getRestResponse().getStatus());
        Thread.sleep(3000);

        // Issue cert
        final PkiResponse issueResponse = vault.pki()
                .issue("testRole", "test.myvault.com", null, null, "1h", CredentialFormat.PEM);
        TestCase.assertNotNull(issueResponse.getCredential().getCertificate());
        TestCase.assertNotNull(issueResponse.getCredential().getPrivateKey());
        TestCase.assertNotNull(issueResponse.getCredential().getSerialNumber());
        TestCase.assertEquals("rsa", issueResponse.getCredential().getPrivateKeyType());
        TestCase.assertNotNull(issueResponse.getCredential().getIssuingCa());
    }

    @Test
    public void testIssueCredentialWithCsr()
            throws VaultException, InterruptedException, NoSuchAlgorithmException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        PublicKey pub = kp.getPublic();
        PrivateKey pvt = kp.getPrivate();
        String csr = null;
        try {
            csr = SSLUtils.generatePKCS10(kp, "", "", "", "", "", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Vault vault = container.getRootVault();

        // Create a role
        final PkiResponse createRoleResponse = vault.pki().createOrUpdateRole("testRole",
                new RoleOptions()
                        .allowedDomains(new ArrayList<String>() {{
                            add("myvault.com");
                        }})
                        .allowSubdomains(true)
                        .maxTtl("9h")
        );
        TestCase.assertEquals(204, createRoleResponse.getRestResponse().getStatus());
        Thread.sleep(3000);

        // Issue cert
        final PkiResponse issueResponse = vault.pki()
                .issue("testRole", "test.myvault.com", null, null, "1h", CredentialFormat.PEM, csr);
        TestCase.assertNotNull(issueResponse.getCredential().getCertificate());
        TestCase.assertNull(issueResponse.getCredential().getPrivateKey());
        TestCase.assertNotNull(issueResponse.getCredential().getSerialNumber());
        TestCase.assertNotNull(issueResponse.getCredential().getIssuingCa());
    }

    @Test
    public void testRevocation()
            throws VaultException, InterruptedException, NoSuchAlgorithmException {
        final Vault vault = container.getRootVault();

        // Create a role
        final PkiResponse createRoleResponse = vault.pki().createOrUpdateRole("testRole",
                new RoleOptions()
                        .allowedDomains(new ArrayList<String>() {{
                            add("myvault.com");
                        }})
                        .allowSubdomains(true)
                        .maxTtl("9h")
        );
        TestCase.assertEquals(204, createRoleResponse.getRestResponse().getStatus());
        Thread.sleep(3000);
        // Issue cert
        final PkiResponse issueResponse = vault.pki()
                .issue("testRole", "test.myvault.com", null, null, "1h", CredentialFormat.PEM);
        TestCase.assertNotNull(issueResponse.getCredential().getSerialNumber());
        vault.pki().revoke(issueResponse.getCredential().getSerialNumber());
    }

    @Test
    public void testCustomMountPath() throws VaultException {
        final Vault vault = container.getRootVault();

        vault.pki("other-pki").createOrUpdateRole("testRole");
        final PkiResponse response = vault.pki("other-pki").getRole("testRole");
        assertTrue(compareRoleOptions(new RoleOptions(), response.getRoleOptions()));
    }

    private boolean compareRoleOptions(final RoleOptions expected, final RoleOptions actual) {
        if (expected.getAllowAnyName() != null && !expected.getAllowAnyName()
                .equals(actual.getAllowAnyName())) {
            return false;
        }
        if (expected.getAllowBareDomains() != null && !expected.getAllowBareDomains()
                .equals(actual.getAllowBareDomains())) {
            return false;
        }
        if (expected.getAllowedDomains() != null) {
            if (!expected.getAllowedDomains().containsAll(actual.getAllowedDomains())
                    || !actual.getAllowedDomains().containsAll(expected.getAllowedDomains())) {
                return false;
            }
        }
        if (expected.getAllowIpSans() != null && !expected.getAllowIpSans()
                .equals(actual.getAllowIpSans())) {
            return false;
        }
        if (expected.getAllowLocalhost() != null && !expected.getAllowLocalhost()
                .equals(actual.getAllowLocalhost())) {
            return false;
        }
        if (expected.getAllowSubdomains() != null && !expected.getAllowSubdomains()
                .equals(actual.getAllowSubdomains())) {
            return false;
        }
        if (expected.getClientFlag() != null && !expected.getClientFlag()
                .equals(actual.getClientFlag())) {
            return false;
        }
        if (expected.getCodeSigningFlag() != null && !expected.getCodeSigningFlag()
                .equals(actual.getCodeSigningFlag())) {
            return false;
        }
        if (expected.getEmailProtectionFlag() != null && !expected.getEmailProtectionFlag()
                .equals(actual.getEmailProtectionFlag())) {
            return false;
        }
        if (expected.getKeyBits() != null && !expected.getKeyBits().equals(actual.getKeyBits())) {
            return false;
        }
        if (expected.getKeyType() != null && !expected.getKeyType().equals(actual.getKeyType())) {
            return false;
        }
        if (expected.getMaxTtl() != null && !expected.getMaxTtl().equals(actual.getMaxTtl())) {
            return false;
        }
        if (expected.getServerFlag() != null && !expected.getServerFlag()
                .equals(actual.getServerFlag())) {
            return false;
        }
        if (expected.getTtl() != null && !expected.getTtl().equals(actual.getTtl())) {
            return false;
        }
        return expected.getUseCsrCommonName() == null || expected.getUseCsrCommonName()
                .equals(actual.getUseCsrCommonName());
    }

}
