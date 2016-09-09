package com.bettercloud.vault.api;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import com.bettercloud.vault.response.SysResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

/**
 * <p>Integration tests for the basic (i.e. "sys") Vault API operations.</p>
 *
 * <p>These tests require a Vault server to be up and running.  See the setup notes in
 * "src/test-integration/README.md".</p>
 */
public class SysTests {

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
    }

    /**
     * Revoke secrets with a prefix and verify that its response status is 204.
     *
     * @throws VaultException
     */
    @Test
    public void testRevokePrefix() throws VaultException {
        final SysResponse sysResponse = vault.sys().put("sys/revoke-prefix/dummy");

        assertEquals(204, sysResponse.getRestResponse().getStatus());
    }

}
