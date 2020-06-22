package com.bettercloud.vault.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.LookupResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.MockVault;
import java.util.Optional;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthLookupTest {

    private static final JsonObject RESPONSE_AUTH_LOOKUPSELF = new JsonObject()
            .add("data", new JsonObject()
                    .add("accessor", "accessor")
                    .add("policies", new JsonArray()));

    private Server server;
    private MockVault vaultServer;

    @Before
    public void before() throws Exception {
        vaultServer = new MockVault(200, RESPONSE_AUTH_LOOKUPSELF.toString());
        server = VaultTestUtils.initHttpMockVault(vaultServer);
        server.start();
    }

    @After
    public void after() throws Exception {
        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void should_lookup_self_use_url_auth_token_lookup_self() throws Exception {
        VaultConfig vaultConfig = new VaultConfig().address("http://127.0.0.1:8999").token("token").build();
        Vault vault = new Vault(vaultConfig);
        LookupResponse response = vault.auth().lookupSelf();

        assertEquals(200, response.getRestResponse().getStatus());

        // Request URL should contain auth/token/lookup-self
        assertEquals(Optional.empty(), vaultServer.getRequestBody());
        assertEquals("token", vaultServer.getRequestHeaders().get("X-Vault-Token"));
        assertEquals("http://127.0.0.1:8999/v1/auth/token/lookup-self", vaultServer.getRequestUrl());

        // Assert response should have the accessor
        assertEquals("accessor", response.getAccessor());
    }

    @Test
    public void should_lookup_self_with_param_use_url_auth_mount_lookup_self() throws Exception {
        VaultConfig vaultConfig = new VaultConfig().address("http://127.0.0.1:8999").token("token").build();
        Vault vault = new Vault(vaultConfig);
        LookupResponse response = vault.auth().lookupSelf("mount");

        assertEquals(200, response.getRestResponse().getStatus());

        // Request URL should contain auth/mount/lookup-self
        assertEquals(Optional.empty(), vaultServer.getRequestBody());
        assertEquals("token", vaultServer.getRequestHeaders().get("X-Vault-Token"));
        assertEquals("http://127.0.0.1:8999/v1/auth/mount/lookup-self", vaultServer.getRequestUrl());

        // Assert response should have the accessor
        assertEquals("accessor", response.getAccessor());
    }

}
