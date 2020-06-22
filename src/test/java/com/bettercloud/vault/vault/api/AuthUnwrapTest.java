package com.bettercloud.vault.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.MockVault;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AuthUnwrapTest {

    private static final JsonObject RESPONSE_AUTH_UNWRAP = new JsonObject()
            .add("renewable", false)
            .add("auth", new JsonObject()
                    .add("policies", new JsonArray())
                    .add("client_token", "unwrappedToken"));

    private Server server;
    private MockVault vaultServer;

    @Before
    public void before() throws Exception {
        vaultServer = new MockVault(200, RESPONSE_AUTH_UNWRAP.toString());
        server = VaultTestUtils.initHttpMockVault(vaultServer);
        server.start();
    }

    @After
    public void after() throws Exception {
        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void should_unwrap_without_param_sends_no_token_and_return_unwrapped_token() throws Exception {
        VaultConfig vaultConfig = new VaultConfig().address("http://127.0.0.1:8999").token("wrappedToken").build();
        Vault vault = new Vault(vaultConfig);
        AuthResponse response = vault.auth().unwrap();

        assertEquals(200, response.getRestResponse().getStatus());

        // Assert request body should NOT have token body (wrapped is in header)
        assertNull(vaultServer.getRequestBody().get().get("token"));
        assertEquals("wrappedToken", vaultServer.getRequestHeaders().get("X-Vault-Token"));

        // Assert response should have the unwrapped token in the client_token key
        assertEquals("unwrappedToken", response.getAuthClientToken());
    }

    @Test
    public void should_unwrap_param_sends_token_and_return_unwrapped_token() throws Exception {
        VaultConfig vaultConfig = new VaultConfig().address("http://127.0.0.1:8999").token("authToken").build();
        Vault vault = new Vault(vaultConfig);
        AuthResponse response = vault.auth().unwrap("wrappedToken");

        assertEquals(200, response.getRestResponse().getStatus());

        // Assert request body SHOULD have token body
        assertEquals("wrappedToken", vaultServer.getRequestBody().get().getString("token",
                null));
        assertEquals("authToken", vaultServer.getRequestHeaders().get("X-Vault-Token"));

        // Assert response should have the unwrapped token in the client_token key
        assertEquals("unwrappedToken", response.getAuthClientToken());
    }

}
