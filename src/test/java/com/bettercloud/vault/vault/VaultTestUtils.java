package com.bettercloud.vault.vault;

import com.bettercloud.vault.vault.mock.MockVault;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * <p>Utilities used by all of the Vault-related unit test classes under
 * <code>src/test/java/com/bettercloud/vault</code>, to setup and shutdown mock Vault server implementations.</p>
 */
public class VaultTestUtils {

    private VaultTestUtils() {
    }

    public static Server initHttpMockVault(final MockVault mock) {
        final Server server = new Server(8999);
        server.setHandler(mock);
        return server;
    }

    public static Server initHttpsMockVault(final MockVault mock) {
        final Server server = new Server();
        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(VaultTestUtils.class.getResource("/keystore.jks").toExternalForm());
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

    public static void shutdownMockVault(final Server server) throws Exception {
        int attemptCount = 0;
        while (!server.isStopped() && attemptCount < 5) {
            attemptCount++;
            server.stop();
            Thread.sleep(1000);
        }
    }

}

