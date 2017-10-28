package com.bettercloud.vault.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.util.VaultContainer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
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

        final Vault vault = container.getVault();
        final AuthResponse response = vault.auth().loginByUserPass(VaultContainer.USER_ID, VaultContainer.PASSWORD);
        NONROOT_TOKEN = response.getAuthClientToken();
    }

    /**
     * Write a secret and verify that it can be read.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndRead() throws VaultException {
        final String path = "secret/hello";
        final String value = "world";
        final Vault vault = container.getRootVault();

        vault.logical().write(path, new HashMap<String, Object>() {{ put("value", value); }});

        final String valueRead = vault.logical().read(path).getData().get("value");
        assertEquals(value, valueRead);
    }

    /**
     * Write a secret and verify that it can be read containing a null value.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndReadNull() throws VaultException {
        final String path = "secret/null";
        final String value = null;
        final Vault vault = container.getRootVault();

        vault.logical().write(path, new HashMap<String, Object>() {{ put("value", value); }});

        final String valueRead = vault.logical().read(path).getData().get("value");
        assertEquals(value, valueRead);
    }

    /**
     * Write a secret, and then verify that its key shows up in the list.
     *
     * @throws VaultException
     */
    @Test
    public void testList() throws VaultException {
        final Vault vault = container.getRootVault();

        vault.logical().write("secret/hello", new HashMap<String, Object>() {{ put("value", "world"); }});

        final List<String> keys = vault.logical().list("secret");
        assertTrue(keys.contains("hello"));
    }

    /**
     * Write a secret, and then verify that is is successfully deleted.
     *
     * @throws VaultException
     */
    @Test
    public void testDelete() throws VaultException {
        final Vault vault = container.getRootVault();

        vault.logical().write("secret/hello", new HashMap<String, Object>() {{ put("value", "world"); }});
        assertTrue(vault.logical().list("secret").contains("hello"));

        vault.logical().delete("secret/hello");
        assertFalse(vault.logical().list("secret").contains("hello"));
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * Tests that exception message includes errors returned by Vault
     *
     * @throws VaultException
     */
    @Test
    public void testReadExceptionMessageIncludesErrorsReturnedByVault() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("permission denied");

        final Vault vault = container.getVault(NONROOT_TOKEN);
        vault.logical().read("secret/null");
    }

    /**
     * Tests that exception message includes errors returned by Vault
     *
     * @throws VaultException
     */
    @Test
    public void testWriteExceptionMessageIncludesErrorsReturnedByVault() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("permission denied");

        final Vault vault = container.getVault(NONROOT_TOKEN);
        vault.logical().write("secret/null", new HashMap<String, Object>() {{ put("value", null); }});
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
        vault.logical().delete("secret/null");
    }

    /**
     * Tests that exception message includes errors returned by Vault
     *
     * @throws VaultException
     */
    @Test
    public void testListExceptionMessageIncludesErrorsReturnedByVault() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("permission denied");

        final Vault vault = container.getVault(NONROOT_TOKEN);
        vault.logical().list("secret/null");
    }

    /**
     * Tests that the various supported data types are correctly marshaled and unmarshaled to and from Vault.
     *
     * @throws VaultException
     */
    @Test
    public void testReadExceptionMessageIncludesErrorsReturnedByVaultOn404() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("{\"errors\":[]}");

        final Vault vault = container.getRootVault();
        final String path = "secret/" + UUID.randomUUID().toString();
        vault.logical().read(path);
    }

    /**
     * Tests that the various supported data types are marshaled/unmarshaled to and from Vault.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndRead_allDataTypes() throws VaultException {
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
}
