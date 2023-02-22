package io.github.jopenlibs.vault.vault.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.response.WrapResponse;
import io.github.jopenlibs.vault.vault.VaultTestUtils;
import io.github.jopenlibs.vault.vault.mock.MockVault;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthWrapTest {

    private static final JsonObject RESPONSE_AUTH_WRAP = new JsonObject()
            .add("renewable", false)
            .add("wrap_info", new JsonObject()
                    .add("token", "wrappedToken")
                    .add("accessor", "accessor_value")
                    .add("ttl", 60)
                    .add("creation_time", "2022-10-09T12:38:27.217414477Z")
                    .add("creation_path", "sys/wrapping/wrap"));

    private Server server;
    private MockVault vaultServer;

    @Before
    public void before() throws Exception {
        vaultServer = new MockVault(200, RESPONSE_AUTH_WRAP.toString());
        server = VaultTestUtils.initHttpMockVault(vaultServer);
        server.start();
    }

    @After
    public void after() throws Exception {
        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void check_wrap_request_response() throws Exception {
        VaultConfig vaultConfig = new VaultConfig().address("http://127.0.0.1:8999")
                .token("wrappedToken").build();
        Vault vault = new Vault(vaultConfig);

        WrapResponse response = vault.auth().wrap(
                new JsonObject()
                        .add("foo", "bar")
                        .add("zoo", "zar"),
                60
        );

        assertEquals(200, response.getRestResponse().getStatus());

        // Assert request body should NOT have token body (wrapped is in header)
        assertEquals("bar", vaultServer.getRequestBody().get().get("foo").asString());
        assertEquals("zar", vaultServer.getRequestBody().get().get("zoo").asString());
        assertEquals("wrappedToken", vaultServer.getRequestHeaders().get("X-Vault-Token"));

        // Assert response should have the unwrapped token in the client_token key
        assertEquals("wrappedToken", response.getToken());
        assertEquals("accessor_value", response.getAccessor());
        assertEquals(60, response.getTtl());
        assertEquals("2022-10-09T12:38:27.217414477Z", response.getCreationTime());
        assertEquals("sys/wrapping/wrap", response.getCreationPath());
    }
}
