package io.github.jopenlibs.vault.vault.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.vault.VaultTestUtils;
import io.github.jopenlibs.vault.vault.mock.MockVault;
import java.util.Optional;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthLookupWrapTest {

    private static final JsonObject RESPONSE_AUTH_LOOKUPSELF = new JsonObject()
            .add("data", new JsonObject()
                    .add("creation_path", "token/path"));

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
    public void should_lookup_wrap_use_url_sys_wrapping_lookup() throws Exception {
        VaultConfig vaultConfig = new VaultConfig().address("http://127.0.0.1:8999")
                .token("wrapped").build();
        Vault vault = new Vault(vaultConfig);
        LogicalResponse response = vault.auth().lookupWrap();

        assertEquals(200, response.getRestResponse().getStatus());

        // Request URL should contain sys/wrapping/lookup
        assertEquals(Optional.empty(), vaultServer.getRequestBody());
        assertEquals("wrapped", vaultServer.getRequestHeaders().get("X-Vault-Token"));
        assertEquals("http://127.0.0.1:8999/v1/sys/wrapping/lookup", vaultServer.getRequestUrl());

        // Assert response should have the accessor
        assertEquals("token/path", response.getData().get("creation_path"));
    }

}
