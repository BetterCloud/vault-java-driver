package com.bettercloud.vault.api;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.util.SSLUtils;
import com.bettercloud.vault.util.VaultContainer;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.HashMap;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.bettercloud.vault.util.TestConstants.PASSWORD;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/**
 * <p>Integration tests for the TLS Certificate auth backend.</p>
 *
 * <p>Note that {@link VaultContainer#getVault()} and the other convenience builders in that class construct
 * {@link Vault} instances that are configured for basic SSL only.  So in order to test client auth, test methods here
 * must manually construct <code>Vault</code> instances themselves.</p>
 */
public class AuthBackendCertTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();
    private static HashMap<String, Object> clientCertAndKey;
    private static String cert;

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        clientCertAndKey = SSLUtils.createClientCertAndKey();
        cert = (String) clientCertAndKey.get("cert");
        container.initAndUnsealVault();
        container.setupBackendCert(cert);
    }

    @Test
    public void testLoginByCert_usingJksConfig() throws VaultException {
        final VaultConfig config =
                new VaultConfig()
                        .address(container.getAddress())
                        .openTimeout(5)
                        .readTimeout(30)
                        .sslConfig(
                                new SslConfig()
                                        .keyStore((KeyStore) clientCertAndKey.get("clientKeystore"), PASSWORD)
                                        .trustStore((KeyStore) clientCertAndKey.get("clientTrustStore"))
                                        .build()
                        )
                        .build();
        final Vault vault = container.getVault(config, VaultContainer.MAX_RETRIES, VaultContainer.RETRY_MILLIS);

        final String token = vault.auth().loginByCert().getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());
    }

    @Test
    public void testLoginByCert_usingPemConfig() throws VaultException {
        final VaultConfig config =
                new VaultConfig()
                        .address(container.getAddress())
                        .openTimeout(5)
                        .readTimeout(30)
                        .sslConfig(
                                new SslConfig()
                                        .pemFile(new File(VaultContainer.CERT_PEMFILE))
                                        .clientPemUTF8(cert)
                                        .clientKeyPemUTF8((String) clientCertAndKey.get("privateKey"))
                                        .build()
                        )
                        .build();
        final Vault vault = container.getVault(config, VaultContainer.MAX_RETRIES, VaultContainer.RETRY_MILLIS);

        final String token = vault.auth().loginByCert().getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());
    }

}
