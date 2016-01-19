package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * These tests are effectively integration tests rather that unit tests, as they require a Vault server to
 * be up and running.
 *
 * TODO: Ensure that these tests are not run as part of the regular unit test suite.
 * TODO: Strategy for dynamically setting the token prior to execution.
 */
public class LogicalTests {

    /**
     * Write a secret and verify that it can be read.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndRead() throws VaultException {
        final String address = System.getProperty("VAULT_ADDR");
        assertNotNull(address);
        final String token = System.getProperty("VAULT_TOKEN");
        assertNotNull(token);

        final String path = "secret/hello";
        final String value = "world";

        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);
        vault.logical().write(path, value);

        assertEquals(value, vault.logical().read(path));
    }

}
