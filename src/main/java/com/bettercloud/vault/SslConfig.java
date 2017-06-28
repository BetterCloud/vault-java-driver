package com.bettercloud.vault;

import lombok.AccessLevel;
import lombok.Getter;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class SslConfig {

    private static final String VAULT_SSL_VERIFY = "VAULT_SSL_VERIFY";
    private static final String VAULT_SSL_CERT = "VAULT_SSL_CERT";
    private static final String VAULT_SSL_CLIENT_CERT = "VAULT_SSL_CLIENT_CERT";
    private static final String VAULT_SSL_CLIENT_KEY = "VAULT_SSL_CLIENT_KEY";

    @Getter private Boolean verify;
    @Getter(AccessLevel.PROTECTED) private String pemUTF8;
    @Getter(AccessLevel.PROTECTED) private String clientPemUTF8;
    @Getter(AccessLevel.PROTECTED) private String clientKeyPemUTF8;
    private EnvironmentLoader environmentLoader;

    /**
     * <p>The code used to load environment variables is encapsulated here, so that a mock version of that environment
     * loader can be used by unit tests.</p>
     *
     * <p>This method is used by unit tests, to inject a mock environment variable when constructing a
     * <code>SslConfig</code> instance using the builder pattern approach rather than the convenience constructor.
     * There really shouldn't ever be a need to call this method outside of a unit test context (hence the
     * <code>protected</code> access level).</p>
     *
     * @param environmentLoader An environment variable loader implementation (presumably a mock)
     * @return This object, with environmentLoader populated, ready for additional builder-pattern method calls or else finalization with the {@link this#build()} method
     */
    protected SslConfig environmentLoader(final EnvironmentLoader environmentLoader) {
        this.environmentLoader = environmentLoader;
        return this;
    }

    /**
     * <p>Whether or not HTTPS connections to the Vault server should verify that a valid SSL certificate is being
     * used.  Unless this is set to <code>false</code>, the default behavior is to always verify SSL certificates.</p>
     *
     * <p>SSL CERTIFICATE VERIFICATION SHOULD NOT BE DISABLED IN PRODUCTION!  This feature is made available to
     * facilitate development or testing environments, where you might be using a self-signed cert that will not
     * pass verification.  However, even if you are using a self-signed cert on your Vault server, you can still leave
     * SSL verification enabled and have your application supply the cert using <code>pemFile()</code>,
     * <code>pemResource()</code>, or <code>pemUTF8()</code>.</p>
     *
     * <p>If no verify is explicitly set, either by this method in a builder pattern approach or else by one of the
     * convenience constructors, then <code>SslConfig</code> will look to the <code>VAULT_SSL_VERIFY</code>
     * environment variable.</p>
     *
     * @param verify Whether or not to verify the SSL certificate used by Vault with HTTPS connections.  Default is <code>true</code>.
     * @return This object, with verify populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public SslConfig verify(final Boolean verify) {
        this.verify = verify;
        return this;
    }

    /**
     * <p>An X.509 certificate, to use when communicating with Vault over HTTPS.  This method accepts a string
     * containing the certificate data.  This string should meet the following requirements:</p>
     *
     * <ul>
     *     <li>Contain an unencrypted X.509 certificate, in PEM format.</li>
     *     <li>Use UTF-8 encoding.</li>
     *     <li>
     *          Contain a line-break between the certificate header (e.g. "-----BEGIN CERTIFICATE-----") and the
     *          rest of the certificate content.  It doesn't matter whether or not there are additional line
     *          breaks within the certificate content, or whether there is a line break before the certificate
     *          footer (e.g. "-----END CERTIFICATE-----").  But the Java standard library will fail to properly
     *          process the certificate without a break following the header
     *          (see http://www.doublecloud.org/2014/03/reading-x-509-certificate-in-java-how-to-handle-format-issue/).
     *      </li>
     * </ul>
     *
     * <p>If no certificate data is provided, either by this method or <code>pemFile()</code>
     * or <code>pemResource()</code>, then <code>SslConfig</code> will look to the
     * <code>VAULT_SSL_CERT</code> environment variable.</p>
     *
     * @param pemUTF8 An X.509 certificate, in unencrypted PEM format with UTF-8 encoding.
     * @return This object, with pemUTF8 populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public SslConfig pemUTF8(final String pemUTF8) {
        this.pemUTF8 = pemUTF8;
        return this;
    }

    /**
     * <p>An X.509 certificate, to use when communicating with Vault over HTTPS.  This method accepts the path of
     * a file containing the certificate data.  This file's contents should meet the following requirements:</p>
     *
     * <ul>
     *     <li>Contain an unencrypted X.509 certificate, in PEM format.</li>
     *     <li>Use UTF-8 encoding.</li>
     *     <li>
     *          Contain a line-break between the certificate header (e.g. "-----BEGIN CERTIFICATE-----") and the
     *          rest of the certificate content.  It doesn't matter whether or not there are additional line
     *          breaks within the certificate content, or whether there is a line break before the certificate
     *          footer (e.g. "-----END CERTIFICATE-----").  But the Java standard library will fail to properly
     *          process the certificate without a break following the header
     *          (see http://www.doublecloud.org/2014/03/reading-x-509-certificate-in-java-how-to-handle-format-issue/).
     *      </li>
     * </ul>
     *
     * <p>If no certificate data is provided, either by this method or <code>pemResource()</code>
     * or <code>pemUTF8()</code>, then <code>SslConfig</code> will look to the
     * <code>VAULT_SSL_CERT</code> environment variable.</p>
     *
     * @param sslPemFile The path of a file containing an X.509 certificate, in unencrypted PEM format with UTF-8 encoding.
     * @return This object, with pemFile populated, ready for additional builder-pattern method calls or else finalization with the build() method
     * @throws VaultException If any error occurs while loading and parsing the PEM file
     */
    public SslConfig pemFile(final File sslPemFile) throws VaultException {
        try (final InputStream input = new FileInputStream(sslPemFile)){
            this.pemUTF8 = VaultConfig.inputStreamToUTF8(input);
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return this;
    }

    /**
     * <p>An X.509 certificate, to use when communicating with Vault over HTTPS.  This method accepts the path of
     * a classpath resource containing the certificate data (e.g. you've bundled the cert into your library or
     * application's JAR/WAR/EAR file).  This resource's contents should meet the following requirements:</p>
     *
     * <ul>
     *     <li>Contain an unencrypted X.509 certificate, in PEM format.</li>
     *     <li>Use UTF-8 encoding.</li>
     *     <li>
     *          Contain a line-break between the certificate header (e.g. "-----BEGIN CERTIFICATE-----") and the
     *          rest of the certificate content.  It doesn't matter whether or not there are additional line
     *          breaks within the certificate content, or whether there is a line break before the certificate
     *          footer (e.g. "-----END CERTIFICATE-----").  But the Java standard library will fail to properly
     *          process the certificate without a break following the header
     *          (see http://www.doublecloud.org/2014/03/reading-x-509-certificate-in-java-how-to-handle-format-issue/).
     *      </li>
     * </ul>
     *
     * <p>If no certificate data is provided, either by this method or <code>pemFile()</code>
     * or <code>pemUTF8()</code>, then <code>SslConfig</code> will look to the
     * <code>VAULT_SSL_CERT</code> environment variable.</p>
     *
     * @param classpathResource The path of a classpath resource containing an X.509 certificate, in unencrypted PEM format with UTF-8 encoding.
     * @return This object, with pemResource populated, ready for additional builder-pattern method calls or else finalization with the build() method
     * @throws VaultException If any error occurs while loading and parsing the PEM file
     */
    public SslConfig pemResource(final String classpathResource) throws VaultException {
        try (final InputStream input = this.getClass().getResourceAsStream(classpathResource)){
            this.pemUTF8 = VaultConfig.inputStreamToUTF8(input);
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return this;
    }

    /**
     * TODO: Document
     *
     * @param clientPemUTF8
     * @return
     */
    public SslConfig clientPemUTF8(final String clientPemUTF8) {
        this.clientPemUTF8 = clientPemUTF8;
        return this;
    }

    /**
     * TODO: Document
     *
     * @param clientPemFile
     * @return
     * @throws VaultException
     */
    public SslConfig clientPemFile(final File clientPemFile) throws VaultException {
        try (final InputStream input = new FileInputStream(clientPemFile)){
            this.clientPemUTF8 = VaultConfig.inputStreamToUTF8(input);
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return this;
    }

    /**
     * TODO: Document
     *
     * @param classpathResource
     * @return
     * @throws VaultException
     */
    public SslConfig clientPemResource(final String classpathResource) throws VaultException {
        try (final InputStream input = this.getClass().getResourceAsStream(classpathResource)){
            this.clientPemUTF8 = VaultConfig.inputStreamToUTF8(input);
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return this;
    }

    /**
     * TODO: Document
     *
     * @param clientKeyPemUTF8
     * @return
     */
    public SslConfig clientKeyPemUTF8(final String clientKeyPemUTF8) {
        this.clientKeyPemUTF8 = clientKeyPemUTF8;
        return this;
    }

    /**
     * TODO: Document
     *
     * @param clientKeyPemFile
     * @return
     * @throws VaultException
     */
    public SslConfig clientKeyPemFile(final File clientKeyPemFile) throws VaultException {
        try (final InputStream input = new FileInputStream(clientKeyPemFile)){
            this.clientKeyPemUTF8 = VaultConfig.inputStreamToUTF8(input);
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return this;
    }

    /**
     * TODO: Document
     *
     * @param classpathResource
     * @return
     * @throws VaultException
     */
    public SslConfig clientKeyPemResource(final String classpathResource) throws VaultException {
        try (final InputStream input = this.getClass().getResourceAsStream(classpathResource)){
            this.clientKeyPemUTF8 = VaultConfig.inputStreamToUTF8(input);
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return this;
    }

    /**
     * TODO: Document
     *
     * @return
     * @throws VaultException
     */
    public SslConfig build() throws VaultException {
        if (this.environmentLoader == null) {
            this.environmentLoader = new EnvironmentLoader();
        }
        if (this.verify == null && environmentLoader.loadVariable(VAULT_SSL_VERIFY) != null) {
            this.verify = Boolean.valueOf(environmentLoader.loadVariable(VAULT_SSL_VERIFY));
        }
        if (this.verify == null) {
            this.verify = true;
        }
        if (this.pemUTF8 == null && environmentLoader.loadVariable(VAULT_SSL_CERT) != null) {
            final File pemFile = new File(environmentLoader.loadVariable(VAULT_SSL_CERT));
            try (final InputStream input = new FileInputStream(pemFile)) {
                this.pemUTF8 = VaultConfig.inputStreamToUTF8(input);
            } catch (IOException e) {
                throw new VaultException(e);
            }
        }
        // TODO:  Setup VAULT_SSL_CLIENT_CERT env variable
        if (this.clientPemUTF8 == null && environmentLoader.loadVariable(VAULT_SSL_CLIENT_CERT) != null) {
            final File pemFile = new File(environmentLoader.loadVariable(VAULT_SSL_CLIENT_CERT));
            try (final InputStream input = new FileInputStream(pemFile)) {
                this.clientPemUTF8 = VaultConfig.inputStreamToUTF8(input);
            } catch (IOException e) {
                throw new VaultException(e);
            }
        }
        return this;
    }

    /**
     * TODO: Document
     * TODO: Move this logic to "build()"?
     *
     * @return
     * @throws VaultException
     */
    public SSLContext getSslContext() throws VaultException {
        if (pemUTF8 == null && clientPemUTF8 == null) {
            return null;
        }
        try {
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            if (pemUTF8 != null) {
                // Convert the trusted servers PEM data into an X509Certificate
                X509Certificate certificate;
                try (final ByteArrayInputStream pem = new ByteArrayInputStream(pemUTF8.getBytes("UTF-8"))) {
                    certificate = (X509Certificate) certificateFactory.generateCertificate(pem);
                }
                // Build a truststore
                final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null);
                keyStore.setCertificateEntry("caCert", certificate);
                trustManagerFactory.init(keyStore);
            }
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            if (clientPemUTF8 != null && clientKeyPemUTF8 != null) {  // TODO: Must they BOTH be non-null?
                // Convert the client certificate PEM data into an X509Certificate
                X509Certificate clientCertificate;
                try (final ByteArrayInputStream pem = new ByteArrayInputStream(clientPemUTF8.getBytes("UTF-8"))) {
                    clientCertificate = (X509Certificate) certificateFactory.generateCertificate(pem);
                }

                // Convert the client private key into a PrivateKey
                final String strippedKey = clientKeyPemUTF8.replace("-----BEGIN PRIVATE KEY-----", "")
                                                     .replace("-----END PRIVATE KEY-----", "");
                final byte[] keyBytes = DatatypeConverter.parseBase64Binary(strippedKey);
                final PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
                final KeyFactory factory = KeyFactory.getInstance("RSA");
                final PrivateKey privateKey = factory.generatePrivate(pkcs8EncodedKeySpec);

                // Build a keystore
                final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, "password".toCharArray());
                keyStore.setCertificateEntry("clientCert", clientCertificate);
                keyStore.setKeyEntry("key", privateKey, "password".toCharArray(), new Certificate[] { clientCertificate });
                keyManagerFactory.init(keyStore, "password".toCharArray()); // TODO
            }

            final KeyManager[] keyManagers = keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers();
            final TrustManager[] trustManagers = trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers();
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException | UnrecoverableKeyException | InvalidKeySpecException e) {
            throw new VaultException(e);
        }
    }

}
