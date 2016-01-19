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

    @Test
    public void testRead() throws VaultException {
        final VaultConfig config = new VaultConfig("http://127.0.0.1:8200", "0aa56b6b-c7fe-72ab-47e1-1d6753d786a8");
        final Vault vault = new Vault(config);
        assertEquals("world", vault.read("secret/hello"));
    }

}
