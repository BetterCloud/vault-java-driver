package com.bettercloud.vault.api;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

import static junit.framework.TestCase.*;

/**
 * Integration tests for the basic (i.e. "logical") Vault API operations.
 *
 * These tests require a Vault server to be up and running.  A server address and token
 * should be passed as JVM properties.  E.g.:
 *
 * <code>gradle integrationTest -DVAULT_ADDR=http://127.0.0.1:8200 -DVAULT_TOKEN=eace6676-4d78-c687-4e54-03cad00e3abf</code>
 *
 */
public class LogicalTests {

    private static final String address = System.getProperty("VAULT_ADDR");
    private static final String token = System.getProperty("VAULT_TOKEN");

    private Vault vault;

    @BeforeClass
    public static void verifyEnv() {
        assertNotNull(address);
        assertNotNull(token);
    }

    @Before
    public void setup() throws VaultException {
        final VaultConfig config = new VaultConfig(address, token);
        vault = new Vault(config);

        // Delete any existing secrets (note: contents within subdirectories must be deleted before the
        // subdirectory itself can be deleted)
        final List<String> existingSecrets = vault.logical().list("secret");
        for (final String secret : existingSecrets) {
            deleteSecretsRecursively("secret/" + secret);
        }
        assertEquals(0, vault.logical().list("secret").size());
    }

    private void deleteSecretsRecursively(final String path) throws VaultException {
        if (path.endsWith("/")) {
            final List<String> existingSecrets = vault.logical().list(path);
            for (final String secret : existingSecrets) {
                deleteSecretsRecursively(path + secret);
            }
        } else {
            vault.logical().delete(path);
        }
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

        vault.logical().write(path, new HashMap<String, String>() {{ put("value", value); }});

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

        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);
        vault.logical().write(path, new HashMap<String, String>() {{ put("value", value); }});

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
        vault.logical().write("secret/hello", new HashMap<String, String>() {{ put("value", "world"); }});

        final List<String> keys = vault.logical().list("secret");
        assertTrue(keys.contains("hello"));
    }

    /**
     * TODO: Document
     *
     * @throws VaultException
     */
    @Test
    public void testDelete() throws VaultException {
        vault.logical().write("secret/hello", new HashMap<String, String>() {{ put("value", "world"); }});
        assertTrue(vault.logical().list("secret").contains("hello"));
        vault.logical().delete("secret/hello");
        assertFalse(vault.logical().list("secret").contains("hello"));
    }

}
