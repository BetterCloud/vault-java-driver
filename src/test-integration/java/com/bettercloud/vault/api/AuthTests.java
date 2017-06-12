package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.response.LookupResponse;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertNull;

/**
 * Integration tests for the basic (i.e. "auth") Vault API operations.
 */
public class AuthTests {

    static String appRoleId;
    static String secretId;

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException, VaultException {
        container.setupAppIdBackend();
        container.setupUserPassBackend();
        container.setupAppRoleBackend();

        final Vault vault = container.getRootVault();

        final LogicalResponse roleIdResponse = vault.logical().read("auth/approle/role/testrole/role-id");
        appRoleId = roleIdResponse.getData().get("role_id");
        final LogicalResponse secretIdResponse = vault.logical().write("auth/approle/role/testrole/secret-id", null);
        secretId = secretIdResponse.getData().get("secret_id");

        assertNotNull(appRoleId);
        assertNotNull(secretId);
    }

    /**
     * Test creation of a new client auth token via a TokenRequest, using the Vault root token
     *
     * @throws VaultException
     */
    @Test
    public void testCreateTokenWithRequest() throws VaultException {
        final Vault vault = container.getRootVault();

        final AuthResponse response = vault.auth().createToken(new Auth.TokenRequest().ttl("1h"));
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
        final Vault vault = container.getVault();
        final String path = "app-id/login";
        final String token = vault.auth().loginByAppID(path, VaultContainer.APP_ID, VaultContainer.USER_ID)
                                  .getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());
    }

    /**
     * Test Authentication with new userpass auth backend
     *
     * @throws VaultException
     */
    @Test
    public void testLoginByUserPass() throws VaultException {
        final Vault vault = container.getVault();
        final AuthResponse response = vault.auth().loginByUserPass(VaultContainer.USER_ID, VaultContainer.PASSWORD);
        final String token = response.getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());
    }

    /**
     * Tests authentication with the app role auth backend
     *
     * @throws VaultException
     */
    @Test
    public void testLoginByAppRole() throws VaultException {
        final Vault vault = container.getVault();
        final String path = "approle";
        final String token = vault.auth().loginByAppRole(path, appRoleId, secretId).getAuthClientToken();

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
        final Vault authVault = container.getRootVault();
        final AuthResponse createResponse = authVault.auth().createToken(new Auth.TokenRequest().ttl("1h"));
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

    /**
     * Tests token lookup-self for the token auth backend.
     *
     * @throws VaultException
     */
    @Test
    public void testLookupSelf() throws VaultException, UnsupportedEncodingException {
        // Generate a client token
        final Vault authVault = container.getRootVault();
        final AuthResponse createResponse = authVault.auth().createToken(new Auth.TokenRequest().ttl("1h"));
        final String token = createResponse.getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());

        // Lookup the client token
        final Vault lookupVault = container.getVault(token);
        final LookupResponse lookupResponse = lookupVault.auth().lookupSelf();

        assertEquals(token, lookupResponse.getId());
        assertEquals(3600, lookupResponse.getCreationTTL());
        assertTrue(lookupResponse.getTTL()<=3600);
    }
}
