package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LookupResponse;
import com.bettercloud.vault.util.VaultContainer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the token auth backend.
 */
public class AuthBackendTokenTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
    }

    /**
     * Test creation of a new client auth token via a TokenRequest, using the Vault root token
     */
    @Test
    public void testCreateTokenWithRequest() throws VaultException {
        final Vault vault = container.getRootVault();

        final AuthResponse response = vault.auth().createToken(
            new Auth.TokenRequest()
            .id(UUID.randomUUID())
            .polices(Arrays.asList("policy"))
            .noParent(true)
            .noDefaultPolicy(false)
            .ttl("1h")
            .displayName("display name")
            .numUses(1L)
            .renewable(true)
            .type("service")
            .explicitMaxTtl("2h")
            .period("2h")
            .entityAlias("entityId")
        );
        final String token = response.getAuthClientToken();
        final String accessor = response.getTokenAccessor();

        assertNotNull(accessor);
        assertNotNull(token);
        assertEquals(2, response.getAuthPolicies().size());
        assertEquals("default", response.getAuthPolicies().get(0));
        assertEquals("policy", response.getAuthPolicies().get(1));
        assertEquals(7200, response.getAuthLeaseDuration());
    }

    /**
     * Tests token self-renewal for the token auth backend.
     */
    @Test
    public void testRenewSelf() throws VaultException {
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

        final String explicitJson = new String(explicitResponse.getRestResponse().getBody(), StandardCharsets.UTF_8);
        final long explicitLeaseDuration = Json.parse(explicitJson).asObject().get("auth").asObject().get("lease_duration").asLong();

        assertEquals(20, explicitLeaseDuration);
    }

    /**
     * Tests token lookup-self for the token auth backend.
     */
    @Test
    public void testLookupSelf() throws VaultException {
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
        assertTrue(lookupResponse.getTTL() <= 3600);
    }

    /**
     * Tests token revoke-self for the token auth backend.
     */
    @Test(expected = VaultException.class)
    public void testRevokeSelf() throws VaultException {
        // Generate a client token
        final Vault authVault = container.getRootVault();
        final AuthResponse createResponse = authVault.auth().createToken(new Auth.TokenRequest().ttl("1h"));
        final String token = createResponse.getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());

        // Revoke the client token
        container.getVault(token).auth().revokeSelf();
        // Lookup the client token
        final Vault lookupVault = container.getVault(token);
        lookupVault.auth().lookupSelf();
    }

}
