package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.response.AuthResponse;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.*;

/**
 * <p>Integration tests for the basic (i.e. "auth") Vault API operations.</p>
 *
 * <p>These tests require a Vault server to be up and running.  See the setup notes in
 * "src/test-integration/README.md".</p>
 */
public class AuthTests {

    final static String appId = "fake_app";
    final static String userId = "fake_user";
    final static String password = "fake_password";
    final static String rootToken = "36303304-3f53-a0c9-af5d-3ffc8dabe683";

    @ClassRule
    public static final VaultContainer container = new VaultContainer(rootToken);

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {
        container.createAuthExample(appId, userId, password);
    }

    /**
     * Test creation of a new client auth token, using the Vault root token
     *
     * @throws VaultException
     */
    @Test
    public void testCreateToken() throws VaultException {
        final Vault vault = container.getRootVault();

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
    public void testLoginByAuthId() throws VaultException, IOException, InterruptedException {
        final String path = "app-id/login";
        final Vault vault = container.getVault();

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
    public void testLoginByUsernamePassword() throws VaultException, IOException, InterruptedException {
        final String path = "userpass/login/" + userId;
        final Vault vault = container.getVault();

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
    public void testRenewSelf() throws VaultException, IOException, InterruptedException {
        // Generate a client token
        final Vault authVault = container.getRootVault();
        final AuthResponse createResponse = authVault.auth().createToken(null, null, null, null, null, "1h", null, null);
        final String token = createResponse.getAuthClientToken();
        assertNotNull(token);
        assertNotSame("", token.trim());

        // Renew the client token
        final Vault renewVault = container.getVault(token);
        final AuthResponse renewResponse = renewVault.auth().renewSelf();
        final String renewToken = renewResponse.getAuthClientToken();
        assertEquals(token, renewToken);

        // Renew the auth token, with an explicit increment value
        final Vault explicitVault = container.getVault(token);
        final AuthResponse explicitResponse = explicitVault.auth().renewSelf(20);
        final String explicitToken = explicitResponse.getAuthClientToken();
        assertEquals(token, explicitToken);
        final String explicitJson = new String(explicitResponse.getRestResponse().getBody(), "UTF-8");
        final long explicitLeaseDuration = Json.parse(explicitJson).asObject().get("auth").asObject().get("lease_duration").asLong();
        assertEquals(20, explicitLeaseDuration);
    }

}
