package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.OpenTimeoutsMockVault;
import com.bettercloud.vault.vault.mock.ReadTimeoutsMockVault;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

/**
 * <p>Unit tests for the Vault driver, having no dependency on an actual Vault server instance being available.  The
 * tests in this class relate to handling network timeouts.</p>
 */
public class TimeoutTests {

    @Test
    public void testOpenTimeout_WithinThreshold() throws Exception {
        // Mock Vault takes 2 seconds to respond
        final OpenTimeoutsMockVault openTimeoutsMockVault = new OpenTimeoutsMockVault(2, 200,
                "{\"data\":{\"value\":\"mock\"}}");
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

    @Test(expected = VaultException.class)
    public void testOpenTimeout_BeyondThreshold() throws Exception {
        // Mock Vault takes 2 seconds to respond
        final OpenTimeoutsMockVault openTimeoutsMockVault = new OpenTimeoutsMockVault(2, 200,
                "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpMockVault(openTimeoutsMockVault);
        server.start();

        // Vault driver is configured to wait only 1 second
        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8998")
                .token("mock_token")
                .openTimeout(1)
                .build();
        final Vault vault = new Vault(vaultConfig);

        // The Vault call should time out
        try {
            final LogicalResponse response = vault.logical().read("secret/hello");
        } catch (Exception e) {
            VaultTestUtils.shutdownMockVault(server);
            throw e;
        }
    }

    @Test
    public void testReadTimeout_WithinThreshold() throws Exception {
        // Mock Vault takes 2 seconds to respond
        final ReadTimeoutsMockVault readTimeoutsMockVault = new ReadTimeoutsMockVault(2, 200,
                "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpMockVault(readTimeoutsMockVault);
        server.start();

        // Vault driver is configured to wait up to 3 seconds
        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .token("mock_token")
                .readTimeout(3)
                .build();
        final Vault vault = new Vault(vaultConfig);

        // The Vault call should succeed
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test(expected = VaultException.class)
    public void testReadTimeout_BeyondThreshold() throws Exception {
        // Mock Vault takes 2 seconds to respond
        final ReadTimeoutsMockVault readTimeoutsMockVault = new ReadTimeoutsMockVault(3, 200,
                "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpMockVault(readTimeoutsMockVault);
        server.start();

        // Vault driver is configured to wait only 1 second
        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .token("mock_token")
                .readTimeout(1)
                .build();
        final Vault vault = new Vault(vaultConfig);

        // The Vault call should time out
        try {
            final LogicalResponse response = vault.logical().read("secret/hello");
        } catch (Exception e) {
            VaultTestUtils.shutdownMockVault(server);
            throw e;
        }
    }

}
