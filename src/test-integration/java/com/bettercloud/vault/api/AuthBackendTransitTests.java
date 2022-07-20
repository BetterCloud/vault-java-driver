package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.transit.DecryptOptions;
import com.bettercloud.vault.api.transit.EncryptOptions;
import com.bettercloud.vault.api.transit.KeyOptions;
import com.bettercloud.vault.response.TransitResponse;
import com.bettercloud.vault.rest.RestResponse;
import com.bettercloud.vault.util.VaultContainer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Integration tests for for operations on Vault's <code>/v1/transit/*</code> REST endpoints.
 */
public class AuthBackendTransitTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
        container.setupBackendTransit();
    }

    //    @Before
    public void setup() throws VaultException {
        final Vault vault = container.getRootVault();

        final TransitResponse defaultResponse = vault.transit().deleteKey("testKey");
        final RestResponse defaultRestResponse = defaultResponse.getRestResponse();
        assertEquals(204, defaultRestResponse.getStatus());

        final TransitResponse customResponse = vault.transit("other-transit").deleteKey("testKey");
        final RestResponse customRestResponse = customResponse.getRestResponse();
        assertEquals(204, customRestResponse.getStatus());
    }

    @Test
    public void testCreateKey_Defaults() throws VaultException {
        final Vault vault = container.getRootVault();

        vault.transit().createKey("testKey");
        final TransitResponse response = vault.transit().getKey("testKey");
        assertTrue(compareKeyOptions(new KeyOptions(), response.getKeyOptions()));
    }

    @Test
    public void testCreateRole_WithOptions() throws VaultException {
        final Vault vault = container.getRootVault();

        final KeyOptions options = new KeyOptions().type("rsa-4096");
        vault.transit().createKey("testKey", options);
        final TransitResponse response = vault.transit().getKey("testKey");
        assertTrue(compareKeyOptions(options, response.getKeyOptions()));
    }

    @Test
    public void encryptDecryptTest() throws VaultException {
        final Vault vault = container.getRootVault();

        testCreateKey_Defaults();

        EncryptOptions options = new EncryptOptions().plaintext("123456789".getBytes());
        final TransitResponse encryptResponse = vault.transit().encryptData("testKey", options);
        DecryptOptions decryptOptions = new DecryptOptions().ciphertext(encryptResponse.getCryptData().getCiphertext());
        final TransitResponse decryptResponse = vault.transit().decryptData("testKey", decryptOptions);
        assertTrue(Arrays.equals(options.getPlaintext(),
                Base64.getDecoder().decode(decryptResponse.getCryptData().getPlaintext())));
    }

    @Test
    public void dataKeyTest() throws VaultException {
        final Vault vault = container.getRootVault();

        testCreateKey_Defaults();

        final TransitResponse encryptResponse = vault.transit().dataKey("plaintext", "testKey");
        DecryptOptions decryptOptions = new DecryptOptions().ciphertext(encryptResponse.getCryptData().getCiphertext());
        final TransitResponse decryptResponse = vault.transit().decryptData("testKey", decryptOptions);
        assertTrue(Arrays.equals(Base64.getDecoder().decode(encryptResponse.getCryptData().getPlaintext()),
                Base64.getDecoder().decode(decryptResponse.getCryptData().getPlaintext())));
    }

//    @Test
//    public void testDeleteKey() throws VaultException {
//        final Vault vault = container.getRootVault();
//
//        testCreateKey_Defaults();
//        final TransitResponse deleteResponse = vault.transit().deleteKey("testKey");
//        assertEquals(204, deleteResponse.getRestResponse().getStatus());
//        final TransitResponse getResponse = vault.transit().getKey("testKey");
//        assertEquals(404, getResponse.getRestResponse().getStatus());
//    }

    //    @Test
//    public void testIssueCredential() throws VaultException, InterruptedException {
//        final Vault vault = container.getRootVault();
//
//        // Create a role
//        final PkiResponse createRoleResponse = vault.pki().createOrUpdateRole("testRole",
//                new RoleOptions()
//                        .allowedDomains(new ArrayList<String>() {{
//                            add("myvault.com");
//                        }})
//                        .allowSubdomains(true)
//                        .maxTtl("9h")
//        );
//        assertEquals(204, createRoleResponse.getRestResponse().getStatus());
//        Thread.sleep(3000);
//
//        // Issue cert
//        final PkiResponse issueResponse = vault.pki().issue("testRole", "test.myvault.com", null, null, "1h", CredentialFormat.PEM);
//        assertNotNull(issueResponse.getCredential().getCertificate());
//        assertNotNull(issueResponse.getCredential().getPrivateKey());
//        assertNotNull(issueResponse.getCredential().getSerialNumber());
//        assertEquals("rsa", issueResponse.getCredential().getPrivateKeyType());
//        assertNotNull(issueResponse.getCredential().getIssuingCa());
//    }
//
//    @Test
//    public void testIssueCredentialWithCsr() throws VaultException, InterruptedException, NoSuchAlgorithmException {
//
//        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
//        kpg.initialize(2048);
//        KeyPair kp = kpg.generateKeyPair();
//        PublicKey pub = kp.getPublic();
//        PrivateKey pvt = kp.getPrivate();
//        String csr = null;
//        try {
//            csr = generatePKCS10(kp, "", "", "", "", "", "");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        final Vault vault = container.getRootVault();
//
//        // Create a role
//        final PkiResponse createRoleResponse = vault.pki().createOrUpdateRole("testRole",
//                new RoleOptions()
//                        .allowedDomains(new ArrayList<String>() {{
//                            add("myvault.com");
//                        }})
//                        .allowSubdomains(true)
//                        .maxTtl("9h")
//        );
//        assertEquals(204, createRoleResponse.getRestResponse().getStatus());
//        Thread.sleep(3000);
//
//        // Issue cert
//        final PkiResponse issueResponse = vault.pki().issue("testRole", "test.myvault.com", null, null, "1h", CredentialFormat.PEM, csr);
//        assertNotNull(issueResponse.getCredential().getCertificate());
//        assertNull(issueResponse.getCredential().getPrivateKey());
//        assertNotNull(issueResponse.getCredential().getSerialNumber());
//        assertNotNull(issueResponse.getCredential().getIssuingCa());
//    }
//
//    @Test
//    public void testRevocation() throws VaultException, InterruptedException, NoSuchAlgorithmException {
//        final Vault vault = container.getRootVault();
//
//        // Create a role
//        final PkiResponse createRoleResponse = vault.pki().createOrUpdateRole("testRole",
//                new RoleOptions()
//                        .allowedDomains(new ArrayList<String>() {{
//                            add("myvault.com");
//                        }})
//                        .allowSubdomains(true)
//                        .maxTtl("9h")
//        );
//        assertEquals(204, createRoleResponse.getRestResponse().getStatus());
//        Thread.sleep(3000);
//        // Issue cert
//        final PkiResponse issueResponse = vault.pki().issue("testRole", "test.myvault.com", null, null, "1h", CredentialFormat.PEM);
//        assertNotNull(issueResponse.getCredential().getSerialNumber());
//        vault.pki().revoke(issueResponse.getCredential().getSerialNumber());
//    }
//
//    @Test
//    public void testCustomMountPath() throws VaultException {
//        final Vault vault = container.getRootVault();
//
//        vault.pki("other-pki").createOrUpdateRole("testRole");
//        final PkiResponse response = vault.pki("other-pki").getRole("testRole");
//        assertTrue(compareRoleOptions(new RoleOptions(), response.getRoleOptions()));
//    }
//
    private boolean compareKeyOptions(final KeyOptions expected, final KeyOptions actual) {
        if (expected.getConvergentEncryption() != null && !expected.getConvergentEncryption()
                .equals(actual.getConvergentEncryption())) {
            return false;
        }
        if (expected.getDerived() != null && !expected.getDerived().equals(actual.getDerived())) {
            return false;
        }
        if (expected.getExportable() != null && !expected.getExportable()
                .equals(actual.getExportable())) {
            return false;
        }
        if (expected.getAllowPlaintextBackup() != null && !expected.getAllowPlaintextBackup()
                .equals(actual.getAllowPlaintextBackup())) {
            return false;
        }
        if (expected.getType() != null && !expected.getType().equals(actual.getType())) {
            return false;
        }
        if (expected.getAutoRotatePeriod() != null && !expected.getAutoRotatePeriod()
                .equals(actual.getAutoRotatePeriod())) {
            return false;
        }
        if (expected.getDeletionAllowed() != null && !expected.getDeletionAllowed()
                .equals(actual.getDeletionAllowed())) {
            return false;
        }
        if (expected.getName() != null && !expected.getName().equals(actual.getName())) {
            return false;
        }
        if (expected.getMinDecryptionVersion() != null && !expected.getMinDecryptionVersion()
                .equals(actual.getMinDecryptionVersion())) {
            return false;
        }
        if (expected.getMinEncryptionVersion() != null && !expected.getMinEncryptionVersion()
                .equals(actual.getMinEncryptionVersion())) {
            return false;
        }
        if (expected.getSupportsEncryption() != null && !expected.getSupportsEncryption()
                .equals(actual.getSupportsEncryption())) {
            return false;
        }
        if (expected.getSupportsDecryption() != null && !expected.getSupportsDecryption()
                .equals(actual.getSupportsDecryption())) {
            return false;
        }
        if (expected.getSupportsDerivation() != null && !expected.getSupportsDerivation()
                .equals(actual.getSupportsDerivation())) {
            return false;
        }
        return expected.getSupportsSigning() == null || expected.getSupportsSigning()
                .equals(actual.getSupportsSigning());
    }

}
