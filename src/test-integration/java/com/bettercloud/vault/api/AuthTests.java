package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;


/**
 * Integration tests for the basic (i.e. "auth") Vault API operations.
 *
 * These tests require a Vault server to be up and running.  A server address and token
 * should be passed as JVM properties.  E.g.:
 *
 * <code>gradle integrationTest -DVAULT_ADDR=http://127.0.0.1:8200
 *
 */
public class AuthTests {

    /**
     * Test Authentication with app-id auth backend
     *
     * @throws VaultException
     */
    @Test
    public void testLoginByAuthId() throws VaultException {
        final String address = System.getProperty("VAULT_ADDR");
        assertNotNull("", address);

        final String path = "app-id/login";
        final String app_id = "foo";
        final String user_id = "bar";
        final VaultConfig config = new VaultConfig(address);
        final Vault vault = new Vault(config);

        assertNotSame("", vault.auth().loginByAppID(path, app_id, user_id).getAuthClientToken());
    }

    /**
     * Test Authentication with userpass auth backend
     *
     * @throws VaultException
     */
    @Test
    public void testLoginByUsernamePassword() throws VaultException {
        final String address = System.getProperty("VAULT_ADDR");
        assertNotNull(address);

        final String path = "userpass/login/test";
        final String password = "foo";

        final VaultConfig config = new VaultConfig(address);
        final Vault vault = new Vault(config);
        assertNotSame("", vault.auth().loginByUsernamePassword(path, password).getAuthClientToken());
    }

}
