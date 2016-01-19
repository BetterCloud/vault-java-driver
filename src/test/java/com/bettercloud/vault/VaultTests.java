package com.bettercloud.vault;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * These tests are effectively integration tests rather that unit tests, as they require a Vault server to
 * be up and running.
 *
 * TODO: Ensure that these tests are not run as part of the regular unit test suite.
 * TODO: Strategy for dynamically setting the token prior to execution.
 */
public class VaultTests {

    /**
     * Write a secret and verify that it can be read.
     *
     * @throws VaultException
     */
    @Test
    public void testWriteAndRead() throws VaultException {
        final String path = "secret/hello";
        final String value = "world";

        final VaultConfig config = new VaultConfig("http://127.0.0.1:8200", "c5543320-1ce3-9511-7c76-b7269e2c56e3");
        final Vault vault = new Vault(config);
        vault.write(path, value);

        assertEquals(value, vault.read(path));
    }

}
