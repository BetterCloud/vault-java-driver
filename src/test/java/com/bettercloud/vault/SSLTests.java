package com.bettercloud.vault;

import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.RetriesMockVault;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

/**
 * TODO: Document
 */
public class SSLTests {

    @Test
    public void testSslVerify_Enabled() throws Exception {
        final RetriesMockVault retriesMockVault = new RetriesMockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(retriesMockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").sslVerify(false).build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");

        assertEquals(5, response.getRetries());
        assertEquals("mock", response.getData().get("value"));

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test(expected = VaultException.class)
    public void testSslVerify_Disabled() throws Exception {
        final RetriesMockVault retriesMockVault = new RetriesMockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(retriesMockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig().address("https://127.0.0.1:9998").token("mock_token").build();
        final Vault vault = new Vault(vaultConfig);

        try {
            final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");
        } catch (Exception e) {
            VaultTestUtils.shutdownMockVault(server);
            throw e;
        }
    }

    @Test
    public void testSslPem_File() throws Exception {
        final RetriesMockVault retriesMockVault = new RetriesMockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(retriesMockVault);
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
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslPem_Resource() throws Exception {
        final RetriesMockVault retriesMockVault = new RetriesMockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(retriesMockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("https://127.0.0.1:9998")
                .token("mock_token")
                .sslPemResource("/cert.pem")
                .build();
        final Vault vault = new Vault(vaultConfig);
        final LogicalResponse response = vault.withRetries(5, 100).logical().read("secret/hello");

        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testSslPem_UTF8() throws Exception {
        final RetriesMockVault retriesMockVault = new RetriesMockVault(5, 200, "{\"data\":{\"value\":\"mock\"}}");
        final Server server = VaultTestUtils.initHttpsMockVault(retriesMockVault);
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

        VaultTestUtils.shutdownMockVault(server);
    }

}
