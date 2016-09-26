package com.bettercloud.vault.api;

import java.util.ArrayList;
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
 * <p>Integration tests for the basic (i.e. "logical") Vault API operations.</p>
 *
 * <p>These tests require a Vault server to be up and running.  See the setup notes in
 * "src/test-integration/README.md".</p>
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
        List<String> existingSecrets = new ArrayList<>();
        try {
             existingSecrets.addAll(vault.logical().list("secret"));
        } catch (VaultException e) {
            if (e.getHttpStatusCode() != 404) {
                throw e;
            }
        }

        for (final String secret : existingSecrets) {
            deleteSecretsRecursively("secret/" + secret);
        }

        existingSecrets = new ArrayList<>();
        try {
            existingSecrets.addAll(vault.logical().list("secret"));
        } catch (VaultException e) {
            if (e.getHttpStatusCode() != 404) {
                throw e;
            }
        }
        assertEquals(0, existingSecrets.size());
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
     * Write a secret, and then verify that is is successfully deleted.
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
