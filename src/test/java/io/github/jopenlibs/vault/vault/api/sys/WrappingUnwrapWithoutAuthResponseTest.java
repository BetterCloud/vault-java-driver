package io.github.jopenlibs.vault.vault.api.sys;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.response.UnwrapResponse;
import io.github.jopenlibs.vault.vault.VaultTestUtils;
import io.github.jopenlibs.vault.vault.mock.MockVault;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WrappingUnwrapWithoutAuthResponseTest {

    private Server server;
    private MockVault vaultServer;

    @After
    public void after() throws Exception {
        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void unwrap_response_without_auth() throws Exception {
        startServer(
                new JsonObject()
                        .add("renewable", false)
                        .add("lease_duration", 0)
                        .add("data", new JsonObject()
                                .add("foo", "bar")
                                .add("zip", "zar"))
                        .toString()
        );

        VaultConfig vaultConfig = new VaultConfig().address("http://127.0.0.1:8999")
                .token("wrappedToken").build();
        Vault vault = new Vault(vaultConfig);
        UnwrapResponse response = vault.sys().wrapping().unwrap("wrappedToken");

        assertEquals(200, response.getRestResponse().getStatus());

        assertEquals("wrappedToken", vaultServer.getRequestHeaders().get("X-Vault-Token"));
        assertEquals("bar", response.getData().get("foo").asString());
        assertEquals("zar", response.getData().get("zip").asString());
    }


    @Test
    public void unwrap_response_without_implicit_null_auth() throws Exception {
        startServer(
                new JsonObject()
                        .add("renewable", false)
                        .add("lease_duration", 0)
                        .add("auth", Json.NULL)
                        .add("data", new JsonObject()
                                .add("foo", "bar")
                                .add("zip", "zar"))
                        .toString()
        );

        VaultConfig vaultConfig = new VaultConfig().address("http://127.0.0.1:8999")
                .token("wrappedToken").build();
        Vault vault = new Vault(vaultConfig);
        UnwrapResponse response = vault.sys().wrapping().unwrap("wrappedToken");

        assertEquals(200, response.getRestResponse().getStatus());

        assertEquals("wrappedToken", vaultServer.getRequestHeaders().get("X-Vault-Token"));
        assertEquals("bar", response.getData().get("foo").asString());
        assertEquals("zar", response.getData().get("zip").asString());
    }

    private void startServer(String mockResponse) throws Exception {
        vaultServer = new MockVault(200, mockResponse);
        server = VaultTestUtils.initHttpMockVault(vaultServer);
        server.start();
    }
}
