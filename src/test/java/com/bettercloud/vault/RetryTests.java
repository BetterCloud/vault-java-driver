package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.RetriesMockVault;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>Unit tests for the Vault driver, having no dependency on an actual Vault server instance being available.  The
 * tests in this class relate to handling of retry logic.</p>
 */
public class RetryTests {

    @Test
    public void testRetries_Read() throws Exception {
        final RetriesMockVault retriesMockVault = new RetriesMockVault(5, 200, "{\"lease_id\":\"12345\",\"renewable\":false,\"lease_duration\":10000,\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpMockVault(retriesMockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "mock_token");
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");
        assertEquals(5, response.getRetries());
        assertEquals("mock", response.getData().get("value"));
        assertEquals("12345", response.getLeaseId());
        assertEquals(false, response.getRenewable());
        assertTrue(10000L == response.getLeaseDuration());

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testRetries_Write() throws Exception {
        final RetriesMockVault retriesMockVault = new RetriesMockVault(5, 204, null);
        final Server server = VaultTestUtils.initHttpMockVault(retriesMockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "mock_token");
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical()
                .write("secret/hello", new HashMap() {{ put("value", "world"); }});
        assertEquals(5, response.getRetries());

        VaultTestUtils.shutdownMockVault(server);
    }

}
