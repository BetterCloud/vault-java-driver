package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.MockVault;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * <p>Unit tests for the Vault driver, having no dependency on an actual Vault server instance being available.  The
 * tests in this class relate to handling of SSL certificates and SSL verification.</p>
 */
public class SSLTests {

    @Test
    public void testSslVerify_Enabled_Get() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").sslVerify(false).build();
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

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

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").build();
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

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

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").sslVerify(false).build();
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().write("secret/hello", new HashMap() {{ put("value", "world"); }});
        assertEquals(204, response.getRestResponse().getStatus());

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test(expected = VaultException.class)
    public void testSslVerify_Disabled_Post() throws Exception {
        final MockVault mockVault = new MockVault(204, null);
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").build();
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

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
        while ( (nextChar = input.read()) != -1 ) {
            output.write( (char) nextChar );
        }
        input.close();
        output.close();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslPemFile(pem)
                .build();
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

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
                .sslPemResource("/cert.pem")
                .build();
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

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
                .sslPemResource("/cert.pem")
                .build();
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical()
                .write("secret/hello", new HashMap() {{ put("value", "world"); }});

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslPem_UTF8() throws Exception {
        final MockVault mockVault = new MockVault(200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(mockVault);
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
        vaultConfig.getSecretEngineVersions().put("secret/", "1");

        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

}
