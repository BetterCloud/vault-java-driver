package com.bettercloud.vault;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


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
}