package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.response.AuthResponse;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static junit.framework.TestCase.assertEquals;
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
        final String appId = System.getProperty("VAULT_APP_ID");
        final String userId = System.getProperty("VAULT_USER_ID");

        assertNotNull(address);
        assertNotNull(appId);
        assertNotNull(userId);

        final String path = "app-id/login";
        final VaultConfig config = new VaultConfig(address);
        final Vault vault = new Vault(config);

        final String token = vault.auth().loginByAppID(path, appId, userId).getAuthClientToken();
        assertNotNull(token);
        assertNotSame("", token.trim());
    }

    /**
     * Test Authentication with userpass auth backend
     *
     * @throws VaultException
     */
    @Test
    public void testLoginByUsernamePassword() throws VaultException {
        final String address = System.getProperty("VAULT_ADDR");
        final String userId = System.getProperty("VAULT_USER_ID");
        final String password = System.getProperty("VAULT_PASSWORD");

        assertNotNull(address);
        assertNotNull(userId);
        assertNotNull(password);

        final String path = "userpass/login/" + userId;
        final VaultConfig config = new VaultConfig(address);
        final Vault vault = new Vault(config);

        final String token = vault.auth().loginByUsernamePassword(path, password).getAuthClientToken();
        assertNotNull(token);
        assertNotSame("", token.trim());
    }

    /**
     * Tests token self-renewal for the token auth backend.
     *
     * @throws VaultException
     */
    @Test
    public void testRenewSelf() throws VaultException, UnsupportedEncodingException {
        // Check environment variables
        final String address = System.getProperty("VAULT_ADDR");
        final String userId = System.getProperty("VAULT_USER_ID");
        final String password = System.getProperty("VAULT_PASSWORD");
        assertNotNull(address);
        assertNotNull(userId);
        assertNotNull(password);

        // Get an initial auth token
        final VaultConfig authConfig = new VaultConfig(address);
        final Vault authVault = new Vault(authConfig);
        final AuthResponse authResponse = authVault.auth().loginByUsernamePassword("userpass/login/" + userId, password);
        final String authToken = authResponse.getAuthClientToken();
        assertNotNull(authToken);
        assertNotSame("", authToken.trim());

        // Renew the auth token
        final VaultConfig renewConfig = new VaultConfig(address, authToken);
        final Vault renewVault = new Vault(renewConfig);
        final AuthResponse renewResponse = renewVault.auth().renewSelf();
        final String renewToken = renewResponse.getAuthClientToken();
        assertEquals(authToken, renewToken);

        // Renew the auth token, with an explicit increment value
        final VaultConfig explicitConfig = new VaultConfig(address, authToken);
        final Vault explicitVault = new Vault(explicitConfig);
        final AuthResponse explicitResponse = explicitVault.auth().renewSelf(20);
        final String explicitToken = explicitResponse.getAuthClientToken();
        assertEquals(authToken, explicitToken);
        final String explicitJson = new String(explicitResponse.getRestResponse().getBody(), "UTF-8");
        final long explicitLeaseDuration = Json.parse(explicitJson).asObject().get("auth").asObject().get("lease_duration").asLong();
        assertEquals(20, explicitLeaseDuration);
    }

}
