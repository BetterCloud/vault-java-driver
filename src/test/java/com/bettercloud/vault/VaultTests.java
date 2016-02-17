package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * <p>Unit tests for the Vault driver, having no dependency on an actual Vault server instance being available.</p>
 */
public class VaultTests {

    @Test
    public void testRetries_Read() throws Exception {
        final MockVault mockVault = new MockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = initHttpMockVault(mockVault);
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
        final MockVault mockVault = new MockVault(5, 204, null);
        final Server server = initHttpMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "mock_token");
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical()
                .write("secret/hello", new HashMap() {{ put("value", "world"); }});
        assertEquals(5, response.getRetries());

        shutdownMockVault(server);
    }

    @Test
    public void testSslVerify_Enabled() throws Exception {
        final MockVault mockVault = new MockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").sslVerify(false).build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");

        assertEquals(5, response.getRetries());
        assertEquals("mock", response.getData().get("value"));

        shutdownMockVault(server);
    }

    @Test(expected = VaultException.class)
    public void testSslVerify_Disabled() throws Exception {
        final MockVault mockVault = new MockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").build();
        final Vault vault = new Vault(vaultConfig);

        try {
            final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");
        } catch (Exception e) {
            shutdownMockVault(server);
            throw e;
        }
    }

    @Test
    public void testSslPem() throws Exception {
        final MockVault mockVault = new MockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = initHttpsMockVault(mockVault);
        server.start();

        final BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/cert.pem")));
        final StringBuilder builder = new StringBuilder();
        String utf8 = "";
        String str;
        while ((str = in.readLine()) != null) {
            utf8 += str + System.lineSeparator();//NOPMD
        }
        in.close();
        final String pemUTF8 = utf8;

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").sslPemUTF8(pemUTF8).build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");

        shutdownMockVault(server);
    }


    private Server initHttpMockVault(final MockVault mock) {
        final Server server = new Server(8999);
        server.setHandler(mock);
        return server;
    }

    private Server initHttpsMockVault(final MockVault mock) {
        final Server server = new Server();
        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(this.getClass().getResource("/keystore.jks").toExternalForm());
        sslContextFactory.setKeyStorePassword("password");
        sslContextFactory.setKeyManagerPassword("password");
        final HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        final ServerConnector sslConnector = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https)
        );
        sslConnector.setPort(9998);
        server.setConnectors(new Connector[] { sslConnector });

        server.setHandler(mock);
        return server;
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

