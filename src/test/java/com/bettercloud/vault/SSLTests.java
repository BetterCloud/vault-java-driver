package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.MockVault;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.util.HashMap;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Vault driver, having no dependency on an actual Vault server instance being available.  The
 * tests in this class relate to handling of SSL certificates and SSL verification.
 */
public class SSLTests {

    @Test
    public void testSslVerify_Enabled_Get() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().verify(false))
                .engineVersion(1)
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        assertEquals(200, response.getRestResponse().getStatus());
        assertEquals("mock", response.getData().get("value"));

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test(expected = VaultException.class)
    public void testSslVerify_Disabled_Get() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .build();
        final Vault vault = new Vault(vaultConfig);

        try {
            final LogicalResponse response = vault.logical().read("secret/hello");
        } catch (Exception e) {
            VaultTestUtils.shutdownMockVault(server);
            throw e;
        }
    }

    @Test
    public void testSslVerify_Enabled_Post() throws Exception {
        final MockVault mockVault = new MockVault(204, null);
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .sslConfig(new SslConfig().pemResource("/cert.pem").build())
                .token("mock_token")
                .build();
        final Vault vault = new Vault(vaultConfig);
        HashMap<String, Object> testMap = new HashMap<>();
        testMap.put("value", "world");
        final LogicalResponse response = vault.logical().write("secret/hello", testMap);
        assertEquals(204, response.getRestResponse().getStatus());

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test(expected = VaultException.class)
    public void testSslVerify_Disabled_Post() throws Exception {
        final MockVault mockVault = new MockVault(204, null);
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").build();
        final Vault vault = new Vault(vaultConfig);

        try {
            final LogicalResponse response = vault.logical().read("secret/hello");
        } catch (Exception e) {
            VaultTestUtils.shutdownMockVault(server);
            throw e;
        }
    }

    @Test
    public void testSslPem_File() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final String tempDirectoryPath = System.getProperty("java.io.tmpdir");
        final File pem = new File(tempDirectoryPath + File.separator + "cert.pem");
        final InputStream input = this.getClass().getResourceAsStream("/cert.pem");
        final FileOutputStream output = new FileOutputStream(pem);
        int nextChar;
        while ((nextChar = input.read()) != -1) {
            output.write((char) nextChar);
        }
        input.close();
        output.close();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().pemFile(pem).build())
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslPem_Resource_Get() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().pemResource("/cert.pem").build())
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslPem_Resource_Post() throws Exception {
        final MockVault mockVault = new MockVault(204, null);
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().pemResource("/cert.pem").build())
                .build();
        final Vault vault = new Vault(vaultConfig);
        HashMap<String, Object> testMap = new HashMap<>();
        testMap.put("value", "world");
        final LogicalResponse response = vault.logical()
                .write("secret/hello", testMap);

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslPem_UTF8() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final BufferedReader in = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/cert.pem")));
        final StringBuilder builder = new StringBuilder();
        StringBuilder utf8 = new StringBuilder();
        String str;
        while ((str = in.readLine()) != null) {
            utf8.append(str).append(System.lineSeparator());//NOPMD
        }
        in.close();
        final String pemUTF8 = utf8.toString();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().pemUTF8(pemUTF8).build())
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslJks_loadTrustStoreFromClasspath() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().trustStoreResource("/keystore.jks").build())
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslJks_loadTrustStoreFromFile() throws Exception {
        // Unfortunately, it's hard to build a filesystem path to the "src/test/resources/keystore.jks" file, because
        // the current working directory might vary depend on how these tests are being run.  So even though it's a
        // hassle, the most reliable approach is to lookup this file as a classpath resource, and copy it to a
        // known location (i.e. the system temp directory).
        final InputStream inputStream = this.getClass().getResourceAsStream("/keystore.jks");
        final String tempDirectoryPath = System.getProperty("java.io.tmpdir");
        final File jks = new File(tempDirectoryPath + File.separator + "keystore.jks");
        final FileOutputStream outputStream = new FileOutputStream(jks);
        final byte[] buffer = new byte[1024];
        int noOfBytes = 0;
        while ((noOfBytes = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, noOfBytes);
        }

        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().trustStoreFile(jks).build())
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslJks_loadTrustStoreFromMemory() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(this.getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().trustStore(trustStore).build())
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslJks_loadKeyStoreAndTrustStore() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslConfig(new SslConfig().trustStoreResource("/keystore.jks").keyStoreResource(
                        "/keystore.jks", "password").build())
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

}
