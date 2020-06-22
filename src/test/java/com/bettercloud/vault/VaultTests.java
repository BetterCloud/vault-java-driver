package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.MockVault;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Test;


/**
 * Unit tests for the various <code>Vault</code> constructors.
 */
public class VaultTests {

    @Test
    public void testDefaultVaultConstructor() {
        VaultConfig vaultConfig = new VaultConfig();
        Vault vault = new Vault(vaultConfig);
        Assert.assertNotNull(vault);
        Assert.assertEquals(String.valueOf(2), vault.logical().getEngineVersionForSecretPath("*").toString());
    }

    @Test
    public void testGlobalEngineVersionVaultConstructor() {
        VaultConfig vaultConfig = new VaultConfig();
        Vault vault = new Vault(vaultConfig, 1);
        Assert.assertNotNull(vault);
        Assert.assertEquals(String.valueOf(1), vault.logical().getEngineVersionForSecretPath("*").toString());
    }

    @Test
    public void testNameSpaceProvidedVaultConstructor() throws VaultException {
        VaultConfig vaultConfig = new VaultConfig().nameSpace("testNameSpace");
        Vault vault = new Vault(vaultConfig, 1);
        Assert.assertNotNull(vault);
    }

    @Test
    public void testNameSpaceProvidedVaultConstructorCannotBeEmpty() {
        try {
            VaultConfig vaultConfig = new VaultConfig().nameSpace("");
        } catch (VaultException e) {
            Assert.assertEquals(e.getMessage(), "A namespace cannot be empty.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidGlobalEngineVersionVaultConstructor() {
        VaultConfig vaultConfig = new VaultConfig();
        Vault vault = new Vault(vaultConfig, 3);
        Assert.assertNull(vault);
    }

    @Test(expected = VaultException.class)
    public void testVaultWithNoKVEnginePathMap() throws VaultException {
        VaultConfig vaultConfig = new VaultConfig();
        Vault vault = new Vault(vaultConfig, true, 1);
        Assert.assertNull(vault);
    }

    @Test(expected = VaultException.class)
    public void testVaultWithEmptyKVEnginePathMap() throws VaultException {
        Map<String, String> emptyEngineKVMap = new HashMap<>();
        VaultConfig vaultConfig = new VaultConfig().secretsEnginePathMap(emptyEngineKVMap);
        Vault vault = new Vault(vaultConfig, true, 1);
        Assert.assertNull(vault);
    }

    @Test
    public void testVaultWithoutKVEnginePathMap() throws VaultException {
        Map<String, String> engineKVMap = new HashMap<>();
        engineKVMap.put("/hello", "2");
        VaultConfig vaultConfig = new VaultConfig().secretsEnginePathMap(engineKVMap);
        Vault vault = new Vault(vaultConfig, false, 1);
        Assert.assertNotNull(vault);
        Assert.assertEquals(String.valueOf(1), vault.logical().getEngineVersionForSecretPath("/hello").toString());
        Assert.assertEquals(String.valueOf(1), vault.logical().getEngineVersionForSecretPath("notInMap").toString());
    }

    @Test
    public void kvEngineMapIsHonored() throws VaultException {
        HashMap<String, String> testMap = new HashMap<>();
        testMap.put("kv-v1/", "1");
        VaultConfig vaultConfig = new VaultConfig().secretsEnginePathMap(testMap);
        Assert.assertNotNull(vaultConfig);
        Vault vault = new Vault(vaultConfig, true, 2);
        Assert.assertNotNull(vault);
        Assert.assertEquals(String.valueOf(1), vault.logical().getEngineVersionForSecretPath("kv-v1").toString());
        Assert.assertEquals(String.valueOf(2), vault.logical().getEngineVersionForSecretPath("notInMap").toString());
    }

    @Test
    public void testConfigBuiler_WithInvalidRequestAsNonError() throws Exception {
        final MockVault mockVault = new MockVault(403, "{\"errors\":[\"preflight capability check returned 403, please ensure client's policies grant access to path \"path/that/does/not/exist/\"]}");
        final Server server = VaultTestUtils.initHttpMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .token("mock_token")
                .build();
        final Vault vault = new Vault(vaultConfig);

        LogicalResponse response = vault.logical().read("path/that/does/not/exist/");
        VaultTestUtils.shutdownMockVault(server);
        Assert.assertEquals(403, response.getRestResponse().getStatus());
        Assert.assertEquals(0, response.getRetries());
    }
}
