package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.response.AuthResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;

/**
 * <p>Integration tests for the basic (i.e. "auth") Vault API operations.</p>
 *
 * <p>These tests require a Vault server to be up and running.  See the setup notes in
 * "src/test-integration/README.md".</p>
 */
public class AuthTests {

    final static String address = System.getProperty("VAULT_ADDR");
    final static String appId = System.getProperty("VAULT_APP_ID");
    final static String userId = System.getProperty("VAULT_USER_ID");
    final static String password = System.getProperty("VAULT_PASSWORD");
    final static String rootToken = System.getProperty("VAULT_TOKEN");

    /**
     * Every test method will need to retrieve Vault credentials from environment variables, but we
     * might as well null-check them once rather than do so redundantly in every method.
     */
    @BeforeClass
    public static void verifyEnv() {
        assertNotNull(address);
        assertNotNull(appId);
        assertNotNull(userId);
        assertNotNull(password);
        assertNotNull(rootToken);
    }

    /**
     * Test creation of a new client auth token, using the Vault root token
     *
     * @throws VaultException
     */
    @Test
    public void testCreateToken() throws VaultException {
        final VaultConfig config = new VaultConfig(address, rootToken);
        final Vault vault = new Vault(config);

        final AuthResponse response = vault.auth().createToken(null, null, null, null, null, "1h", null, null);
        final String token = response.getAuthClientToken();
        assertNotNull(token);
    }

    /**
     * Test Authentication with app-id auth backend
     *
     * @throws VaultException
     */
    @Test
    public void testLoginByAuthId() throws VaultException {
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
        // Generate a client token
        final VaultConfig authConfig = new VaultConfig(address, rootToken);
        final Vault authVault = new Vault(authConfig);
        final AuthResponse createResponse = authVault.auth().createToken(null, null, null, null, null, "1h", null, null);
        final String token = createResponse.getAuthClientToken();
        assertNotNull(token);
        assertNotSame("", token.trim());

        // Renew the client token
        final VaultConfig renewConfig = new VaultConfig(address, token);
        final Vault renewVault = new Vault(renewConfig);
        final AuthResponse renewResponse = renewVault.auth().renewSelf();
        final String renewToken = renewResponse.getAuthClientToken();
        assertEquals(token, renewToken);

        // Renew the auth token, with an explicit increment value
        final VaultConfig explicitConfig = new VaultConfig(address, token);
        final Vault explicitVault = new Vault(explicitConfig);
        final AuthResponse explicitResponse = explicitVault.auth().renewSelf(20);
        final String explicitToken = explicitResponse.getAuthClientToken();
        assertEquals(token, explicitToken);
        final String explicitJson = new String(explicitResponse.getRestResponse().getBody(), "UTF-8");
        final long explicitLeaseDuration = Json.parse(explicitJson).asObject().get("auth").asObject().get("lease_duration").asLong();
        assertEquals(20, explicitLeaseDuration);
    }

}
