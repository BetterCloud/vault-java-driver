package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.OpenTimeoutsMockVault;
import org.eclipse.jetty.server.Server;
import org.junit.Ignore;
import org.junit.Test;

/**
 * TODO: Document
 */
public class TimeoutTests {

    @Test
    public void testOpenTimeout_WithinThreshold() throws Exception {
        // Mock Vault takes 2 seconds to respond
        final OpenTimeoutsMockVault openTimeoutsMockVault = new OpenTimeoutsMockVault(2, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpMockVault(openTimeoutsMockVault);
        server.start();

        // Vault driver is configured to wait up to 3 seconds
        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .token("mock_token")
                .openTimeout(3)
                .build();
        final Vault vault = new Vault(vaultConfig);

        // The Vault call should succeed
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    @Ignore
    public void testOpenTimeout_BeyondThreshold() throws Exception {
        // Mock Vault takes 2 seconds to respond
        final OpenTimeoutsMockVault openTimeoutsMockVault = new OpenTimeoutsMockVault(3, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpMockVault(openTimeoutsMockVault);
        server.start();

        // Vault driver is configured to wait only 1 second
        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8998")
                .token("mock_token")
                .openTimeout(5)
                .build();
        final Vault vault = new Vault(vaultConfig);

        // The Vault call should time out
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);

    }

}
