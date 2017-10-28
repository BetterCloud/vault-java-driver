package com.bettercloud.vault.api;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.util.SSLUtils;
import com.bettercloud.vault.util.VaultContainer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.shaded.org.bouncycastle.operator.OperatorCreationException;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;

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

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException, CertificateException, SignatureException,
            NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, NoSuchProviderException,
            InvalidKeyException {
        container.initAndUnsealVault();
        SSLUtils.createClientCertAndKey();
        container.setupBackendCert();
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
                                        .keyStoreFile(new File(VaultContainer.CLIENT_KEYSTORE), "password")
                                        .trustStoreFile(new File(VaultContainer.CLIENT_TRUSTSTORE))
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
                                        .clientPemFile(new File(VaultContainer.CLIENT_CERT_PEMFILE))
                                        .clientKeyPemFile(new File(VaultContainer.CLIENT_PRIVATE_KEY_PEMFILE))
                                        .build()
                        )
                        .build();
        final Vault vault = container.getVault(config, VaultContainer.MAX_RETRIES, VaultContainer.RETRY_MILLIS);

        final String token = vault.auth().loginByCert().getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());
    }

}
