package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.*;

/**
 * <p>Integration tests for the basic (i.e. "logical") Vault API operations.</p>
 *
 * <p>These tests require a Vault server to be up and running.  See the setup notes in
 * "src/test-integration/README.md".</p>
 */
public class LogicalTests {

    final static String rootToken = "36303304-3f53-a0c9-af5d-3ffc8dabe683";

    @ClassRule
    public static final VaultContainer container = new VaultContainer(rootToken);


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

        final Vault vault = container.getRootVault();
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
        final Vault vault = container.getRootVault();

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
        final Vault vault = container.getRootVault();

        vault.logical().write("secret/hello", new HashMap<String, String>() {{ put("value", "world"); }});
        assertTrue(vault.logical().list("secret").contains("hello"));
        vault.logical().delete("secret/hello");
        assertFalse(vault.logical().list("secret").contains("hello"));
    }

}
