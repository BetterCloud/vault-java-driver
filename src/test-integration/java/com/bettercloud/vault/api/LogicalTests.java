package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.util.VaultContainer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Integration tests for the basic (i.e. "logical") Vault API operations.
 */
public class LogicalTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    private static String NONROOT_TOKEN;

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException, VaultException {
        container.initAndUnsealVault();
        container.setupBackendUserPass();
        container.setEngineVersions();
        final Vault vault = container.getVault();
        final AuthResponse response = vault.auth().loginByUserPass(VaultContainer.USER_ID, VaultContainer.PASSWORD);
        NONROOT_TOKEN = response.getAuthClientToken();
    }

    /**
     * Write a secret and verify that it can be read, using KV Secrets engine version 2.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndRead() throws VaultException {
        final String pathToWrite = "secret/hello";
        final String pathToRead = "secret/hello";

        final String value = "world";
        final Vault vault = container.getRootVault();

        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", value);

        vault.logical().write(pathToWrite, testMap);

        final String valueRead = vault.logical().read(pathToRead).getData().get("value");
        assertEquals(value, valueRead);
    }

    /**
     * Write a secret and verify that it can be read, using KV Secrets engine version 1.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndReadKVEngineV1() throws VaultException {
        final String pathToWrite = "kv-v1/hello";
        final String pathToRead = "kv-v1/hello";

        final String value = "world";
        final Vault vault = container.getRootVaultWithCustomVaultConfig(new VaultConfig().engineVersion(1));

        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", value);

        vault.logical().write(pathToWrite, testMap);

        final String valueRead = vault.logical().read(pathToRead).getData().get("value");
        assertEquals(value, valueRead);
    }


    /**
     * Write a secret and verify that a specific version can be read, using KV Secrets Engine version 2.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndReadSpecificVersions() throws VaultException {
        final String pathToWrite = "secret/hello";
        final String pathToRead = "secret/hello";

        final String value = "world";
        final Vault vault = container.getRootVault();

        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", value);

        vault.logical().write(pathToWrite, testMap);

        final String valueRead = vault.logical().read(pathToRead, true, 1).getData().get("value");
        assertEquals(value, valueRead);
    }


    /**
     * Write a secret and verify that it can be read containing a null value, using KV Engine version 2.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndReadNull() throws VaultException {
        final String pathToWrite = "secret/null";
        final String pathToRead = "secret/null";
        final String value = null;
        final Vault vault = container.getRootVault();

        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", value);

        vault.logical().write(pathToWrite, testMap);

        final String valueRead = vault.logical().read(pathToRead).getData().get("value");
        assertEquals(value, valueRead);
    }

    /**
     * Write a secret and verify that it can be read containing a null value, using KV Engine version 1.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndReadNullKVEngineV1() throws VaultException {
        final String pathToWrite = "kv-v1/null";
        final String pathToRead = "kv-v1/null";
        final String value = null;
        final Vault vault = container.getRootVaultWithCustomVaultConfig(new VaultConfig().engineVersion(1));

        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", value);

        vault.logical().write(pathToWrite, testMap);

        final String valueRead = vault.logical().read(pathToRead).getData().get("value");
        assertEquals(value, valueRead);
    }

    /**
     * Write a secret, and then verify that its key shows up in the list, using KV Engine version 2.
     *
     * @throws VaultException
     */
    @Test
    public void testList() throws VaultException {
        final Vault vault = container.getRootVault();
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", "world");

        vault.logical().write("secret/hello", testMap);
        final List<String> keys = vault.logical().list("secret").getListData();
        assertTrue(keys.contains("hello"));
    }

    /**
     * Write a secret, and then verify that its key shows up in the list, using KV Engine version 1.
     *
     * @throws VaultException
     */
    @Test
    public void testListKVEngineV1() throws VaultException {
        final Vault vault = container.getRootVaultWithCustomVaultConfig(new VaultConfig().engineVersion(1));
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", "world");

        vault.logical().write("kv-v1/hello", testMap);
        final List<String> keys = vault.logical().list("kv-v1").getListData();
        assertTrue(keys.contains("hello"));
    }

    /**
     * Write a secret, and then verify that is is successfully deleted, using KV Engine version 2.
     *
     * @throws VaultException
     */
    @Test
    public void testDelete() throws VaultException {
        final Vault vault = container.getRootVault();
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", "world");

        vault.logical().write("secret/hello", testMap);
        assertTrue(vault.logical().list("secret").getListData().contains("hello"));
        vault.logical().delete("secret/hello");
        assertFalse(vault.logical().list("secret").getListData().contains("hello"));
    }

    /**
     * Write a secret, and then verify that is is successfully deleted, using KV Engine version 1.
     *
     * @throws VaultException
     */
    @Test
    public void testDeleteKVEngineV1() throws VaultException {
        final Vault vault = container.getRootVaultWithCustomVaultConfig(new VaultConfig().engineVersion(1));
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", "world");

        vault.logical().write("kv-v1/hello", testMap);
        assertTrue(vault.logical().list("kv-v1").getListData().contains("hello"));
        vault.logical().delete("kv-v1/hello");
        assertFalse(vault.logical().list("kv-v1").getListData().contains("hello"));
    }

    /**
     * Write a secret multiple times, and have multiple versions of the secret, and then verify that the specified version of
     * them are successfully destroyed.
     *
     * @throws VaultException
     */
    @Test
    public void testDestroy() throws VaultException {
        final Vault vault = container.getRootVault();
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", "world");

        vault.logical().write("secret/hello", testMap);
        vault.logical().write("secret/hello", testMap);
        vault.logical().write("secret/hello", testMap);
        assertTrue(vault.logical().read("secret/hello").getData().containsKey("value"));
        vault.logical().destroy("secret/hello", new int[]{1});
        assertTrue(vault.logical().read("secret/hello").getData().containsKey("value"));
        try {
            vault.logical().read("secret/hello", true, 1);
        } catch (VaultException e) {
            Assert.assertEquals(e.getHttpStatusCode(), 404);
        }
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * Tests that exception message includes errors returned by Vault
     *
     * @throws VaultException
     */
    @Test
    public void testReadPermissionDeniedReturnedByVault() throws VaultException {
        final Vault vault = container.getVault(NONROOT_TOKEN);
        LogicalResponse read = vault.logical().read("secret/null");
        assertEquals(403, read.getRestResponse().getStatus());
    }

    /**
     * Tests that exception message includes errors returned by Vault
     *
     * @throws VaultException
     */
    @Test
    public void testWritePermissionDeniedReturnedByVault() throws VaultException {
        final Vault vault = container.getVault(NONROOT_TOKEN);
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", null);
        LogicalResponse write = vault.logical().write("secret/null", testMap);
        assertEquals(403, write.getRestResponse().getStatus());
    }

    /**
     * Tests that exception message includes errors returned by Vault
     *
     * @throws VaultException
     */
    @Test
    public void testDeleteExceptionMessageIncludesErrorsReturnedByVault() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("permission denied");

        final Vault vault = container.getVault(NONROOT_TOKEN);
        LogicalResponse delete = vault.logical().delete("secret/null");
        assertEquals(403, delete.getRestResponse().getStatus());
    }

    /**
     * Tests that exception message includes errors returned by Vault
     *
     * @throws VaultException
     */
    @Test
    public void testListPermissionDeniedReturnedByVault() throws VaultException {
        final Vault vault = container.getVault(NONROOT_TOKEN);
        LogicalResponse response = vault.logical().list("secret/null");
        assertEquals(403, response.getRestResponse().getStatus());
    }

    /**
     * Tests that the various supported data types are correctly marshaled and unmarshaled to and from Vault.
     *
     * @throws VaultException
     */
    @Test
    public void testReadReturnedByVaultOn404() throws VaultException {
        final Vault vault = container.getRootVault();
        final String path = "secret/" + UUID.randomUUID().toString();
        LogicalResponse read = vault.logical().read(path);
        assertEquals(404, read.getRestResponse().getStatus());
    }

    /**
     * Tests that the various supported data types are marshaled/unmarshaled to and from Vault, using KV Engine version 2.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndReadAllDataTypes() throws VaultException {
        final String path = "secret/hello";

        final Map<String, Object> nameValuePairs = new HashMap<>();
        nameValuePairs.put("testBoolean", true);
        nameValuePairs.put("testInt", 1001);
        nameValuePairs.put("testFloat", 123.456);
        nameValuePairs.put("testString", "Hello world!");
        nameValuePairs.put("testObject", "{ \"nestedBool\": true, \"nestedInt\": 123, \"nestedFloat\": 123.456, \"nestedString\": \"foobar\", \"nestedArray\": [\"foo\", \"bar\"], \"nestedObject\": { \"foo\": \"bar\" } }");

        final Vault vault = container.getRootVault();
        vault.logical().write(path, nameValuePairs);

        final Map<String, String> valuesRead = vault.logical().read(path).getData();
        for (final Map.Entry<String, String> entry : valuesRead.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }

    /**
     * Tests that the various supported data types are marshaled/unmarshaled to and from Vault, using KV Engine version 1.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndReadAllDataTypesKVEngineV1() throws VaultException {
        final String path = "kv-v1/hello";

        final Map<String, Object> nameValuePairs = new HashMap<>();
        nameValuePairs.put("testBoolean", true);
        nameValuePairs.put("testInt", 1001);
        nameValuePairs.put("testFloat", 123.456);
        nameValuePairs.put("testString", "Hello world!");
        nameValuePairs.put("testObject", "{ \"nestedBool\": true, \"nestedInt\": 123, \"nestedFloat\": 123.456, \"nestedString\": \"foobar\", \"nestedArray\": [\"foo\", \"bar\"], \"nestedObject\": { \"foo\": \"bar\" } }");

        final Vault vault = container.getRootVaultWithCustomVaultConfig(new VaultConfig().engineVersion(1));
        vault.logical().write(path, nameValuePairs);

        final Map<String, String> valuesRead = vault.logical().read(path).getData();
        for (final Map.Entry<String, String> entry : valuesRead.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }

    /**
     * Tests that the Vault KV Engine Version API call is successful when attempting to discover the KV Engine versions.
     *
     * @throws VaultException
     */
    @Test
    public void testVaultKVEnginePathsCanBeDiscovered() throws VaultException {
        final Vault vault = container.getRootVault();
        Map<String, String> secretPaths = vault.getSecretEngineVersions();
        Assert.assertEquals(secretPaths.get("secret/"), "2");
        Assert.assertEquals(secretPaths.get("kv-v1/"), "1");
        Assert.assertNull(secretPaths.get("notInMap"));
    }

    /**
     * Tests that a specific version of a secret can be deleted.
     *
     * @throws VaultException
     */
    @Test
    public void testVaultDeleteASpecificVersion() throws VaultException {
        final String pathToWrite = "secret/hello";
        final String pathToRead = "secret/hello";
        final String version1Value = "world";
        final String version2Value = "world2";
        final Vault vault = container.getRootVault();
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", version1Value);
        vault.logical().write(pathToWrite, testMap);
        testMap.put("value", version2Value);
        vault.logical().write(pathToWrite, testMap);
        vault.logical().delete(pathToWrite, new int[]{1});
        try {
            vault.logical().read(pathToRead, true, 1).getData().get("value");
        } catch (VaultException e) {
            Assert.assertEquals(e.getHttpStatusCode(), 404);
        }
        final String valueRead = vault.logical().read(pathToRead, true, 2).getData().get("value");
        Assert.assertEquals(valueRead, version2Value);
    }

    /**
     * Tests that a specific version of a secret can be undeleted.
     *
     * @throws VaultException
     */
    @Test
    public void testVaultUnDeleteASpecificVersion() throws VaultException {
        final String pathToWrite = "secret/hello";
        final String pathToRead = "secret/hello";
        final String version1Value = "world";
        final Vault vault = container.getRootVault();
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", version1Value);
        vault.logical().write(pathToWrite, testMap);
        vault.logical().delete(pathToWrite, new int[]{1});
        try {
            vault.logical().read(pathToRead, true, 1).getData().get("value");
        } catch (VaultException e) {
            Assert.assertEquals(e.getHttpStatusCode(), 404);
        }
        vault.logical().unDelete(pathToRead, new int[]{1});
        final String valueRead = vault.logical().read(pathToRead, true, 1).getData().get("value");
        Assert.assertEquals(valueRead, version1Value);
    }

    /**
     * Tests that a specific KV engine can be upgraded from Version 1 to Version 2.
     *
     * @throws VaultException
     */
    @Test
    public void testVaultUpgrade() throws VaultException {
        final String kvToUpgrade = "kv-v1-Upgrade-Test/";
        final Vault vault = container.getRootVaultWithCustomVaultConfig(new VaultConfig().engineVersion(1));
        String kVOriginalVersion = vault.getSecretEngineVersions().get("kv-v1-Upgrade-Test/");
        vault.logical().upgrade(kvToUpgrade);
        String kVUpgradedVersion = vault.getSecretEngineVersions().get("kv-v1-Upgrade-Test/");
        Assert.assertEquals(kVOriginalVersion, "1");
        Assert.assertEquals(kVUpgradedVersion, "2");
    }
}
