package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TODO: Document...
 */
public class VaultTests {

    @Test
    public void testRetries_Read() throws Exception {
        final Server server = new Server(8999);
        server.setHandler( new MockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}") );
        server.start();

        final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "mock_token");
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");
        assertEquals(5, response.getRetries());
        assertEquals("mock", response.getData().get("value"));

        shutdownMockVault(server);
    }

    @Test
    public void testRetries_Write() throws Exception {
        final Server server = new Server(8999);
        server.setHandler( new MockVault(5, 204, null) );
        server.start();

        final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "mock_token");
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical().write("secret/hello", "world");
        assertEquals(5, response.getRetries());

        shutdownMockVault(server);
    }

    private void shutdownMockVault(final Server server) throws Exception {
        int attemptCount = 0;
        while (!server.isStopped() && attemptCount < 5) {
            attemptCount++;
            server.stop();
            Thread.sleep(1000);
        }
    }

}

