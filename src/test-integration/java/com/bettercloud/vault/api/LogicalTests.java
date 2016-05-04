package com.bettercloud.vault.api;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

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

    @Before
    public void setUp() throws VaultException {
        assertNotNull(address);
        assertNotNull(token);

        final VaultConfig config = new VaultConfig(address, token);
        vault = new Vault(config);
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

}
