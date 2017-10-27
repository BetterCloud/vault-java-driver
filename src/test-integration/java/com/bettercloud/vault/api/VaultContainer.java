package com.bettercloud.vault.api;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Capability;
import org.bouncycastle.openssl.PEMReader;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.HttpWaitStrategy;
import org.testcontainers.shaded.org.bouncycastle.asn1.x500.X500Name;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.Extension;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralNames;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.testcontainers.shaded.org.bouncycastle.cert.X509CertificateHolder;
import org.testcontainers.shaded.org.bouncycastle.cert.X509v3CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.testcontainers.shaded.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.testcontainers.shaded.org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testcontainers.shaded.org.bouncycastle.operator.ContentSigner;
import org.testcontainers.shaded.org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.testcontainers.shaded.org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.testcontainers.shaded.org.bouncycastle.operator.OperatorCreationException;
import org.testcontainers.shaded.org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.testcontainers.shaded.org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

/** Sets up and exposes utilities for dealing with a Docker-hosted instance of Vault, for integration tests. */
public class VaultContainer implements TestRule {

    public static final String APP_ID = "fake_app";
    public static final String USER_ID = "fake_user";
    public static final String PASSWORD = "fake_password";

    public static final String CURRENT_WORKING_DIRECTORY = System.getProperty("user.dir");
    public static final String SSL_DIRECTORY = CURRENT_WORKING_DIRECTORY + File.separator + "ssl";
    public static final String CERT_PEMFILE = SSL_DIRECTORY + File.separator + "root-cert.pem";
//    public static final String CERT_PEMFILE = SSL_DIRECTORY + File.separator + "vault-cert.pem";


    public static final String PRIVATE_KEY_PEMFILE = SSL_DIRECTORY + File.separator + "vault-privkey.pem";
    public static final String CLIENT_CERT_PEMFILE = SSL_DIRECTORY + File.separator + "client-cert.pem";
    public static final String CLIENT_PRIVATE_KEY_PEMFILE = SSL_DIRECTORY + File.separator + "client-privatekey.pem";
    public static final String CLIENT_KEYSTORE = SSL_DIRECTORY + File.separator + "keystore.jks";
    public static final String CLIENT_TRUSTSTORE = SSL_DIRECTORY + File.separator + "truststore.jks";

    public static final String CONTAINER_STARTUP_SCRIPT = "/vault/config/startup.sh";
    public static final String CONTAINER_CONFIG_FILE = "/vault/config/config.json";
    public static final String CONTAINER_OPENSSL_CONFIG_FILE = "/vault/config/ssl/openssl.conf";
    public static final String CONTAINER_SSL_DIRECTORY = "/vault/config/ssl";
    public static final String CONTAINER_CERT_PEMFILE = CONTAINER_SSL_DIRECTORY + "/vault-cert.pem";
    public static final String CONTAINER_CLIENT_CERT_PEMFILE = CONTAINER_SSL_DIRECTORY + "/client-cert.pem";

    public static final int MAX_RETRIES = 5;
    public static final int RETRY_MILLIS = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultContainer.class);

    private final GenericContainer container;

    private X509Certificate vaultCertificate;

    private String rootToken;
    private String unsealKey;

    /** Establishes a running Docker container, hosting a Vault server instance. */
    public VaultContainer() {
        container = new GenericContainer("vault:0.8.3")
                .withClasspathResourceMapping("/startup.sh", CONTAINER_STARTUP_SCRIPT, BindMode.READ_ONLY)
                .withClasspathResourceMapping("/config.json", CONTAINER_CONFIG_FILE, BindMode.READ_ONLY)
                .withClasspathResourceMapping("/openssl.conf", CONTAINER_OPENSSL_CONFIG_FILE, BindMode.READ_ONLY)
                .withFileSystemBind(SSL_DIRECTORY, CONTAINER_SSL_DIRECTORY, BindMode.READ_WRITE)
                .withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
                    // TODO: Why does the compiler freak out when this anonymous class is converted to a lambda?
                    @Override
                    public void accept(final CreateContainerCmd createContainerCmd) {
                        createContainerCmd.withCapAdd(Capability.IPC_LOCK);
                    }
                })
                .withNetworkMode("host")  // .withExposedPorts(8200)
                .withCommand("/bin/sh " + CONTAINER_STARTUP_SCRIPT)
                .waitingFor(
                        new CustomWaitStrategy()
                                .forPath("/v1/sys/seal-status")
                                .forStatusCode(HttpURLConnection.HTTP_BAD_REQUEST) // expected response when "vault init" has not yet run
                );
    }

    class CustomWaitStrategy extends HttpWaitStrategy {
        @Override
        protected Integer getLivenessCheckPort() {
            return 8280;
        }
    }

    /**
     * Called by JUnit automatically after the constructor method.  Launches the Docker container that was configured
     * in the constructor.
     *
     * @param base
     * @param description
     * @return
     */
    @Override
    public Statement apply(final Statement base, final Description description) {
        return container.apply(base, description);
    }

    /**
     * To be called by a test class method annotated with {@link org.junit.BeforeClass}.  This logic doesn't work
     * when placed inside of the constructor or {@link this#apply(Statement, Description)} methods here, presumably
     * because the Docker container spawned by TestContainers is not ready to accept commonds until after those
     * methods complete.
     *
     * This method initializes the Vault server, capturing the unseal key and root token that are displayed on the
     * console.  It then uses the key to unseal the Vault instance, and stores the token in a member field so it
     * will be available to other methods.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void initAndUnsealVault() throws IOException, InterruptedException {

        final Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);
        container.followOutput(logConsumer);

        // Initialize the Vault server
        final Container.ExecResult initResult = runCommand("vault", "init", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "-key-shares=1", "-key-threshold=1");
        final String[] initLines = initResult.getStdout().split(System.lineSeparator());
        this.unsealKey = initLines[0].replace("Unseal Key 1: ", "");
        this.rootToken = initLines[1].replace("Initial Root Token: ", "");

        System.out.println("Root token: " + rootToken.toString());

        // Unseal the Vault server
        runCommand("vault", "unseal", "-ca-cert=" + CONTAINER_CERT_PEMFILE, unsealKey);

        try {
            createClientCertAndKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepares the Vault server for testing of the AppID auth backend (i.e. mounts the backend and populates test
     * data).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupBackendAppId() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_CERT_PEMFILE, rootToken);

        runCommand("vault", "auth-enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "app-id");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "auth/app-id/map/app-id/" + APP_ID, "display_name=" + APP_ID);
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "auth/app-id/map/user-id/" + USER_ID, "value=" + APP_ID);
    }

    /**
     * Prepares the Vault server for testing of the Username and Password auth backend (i.e. mounts the backend and
     * populates test data).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupBackendUserPass() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_CERT_PEMFILE, rootToken);

        runCommand("vault", "auth-enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "userpass");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "auth/userpass/users/" + USER_ID, "password=" + PASSWORD);
    }

    /**
     * Prepares the Vault server for testing of the AppRole auth backend (i.e. mounts the backend and populates test
     * data).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupBackendAppRole() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_CERT_PEMFILE, rootToken);

        runCommand("vault", "auth-enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "approle");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "auth/approle/role/testrole",
                   "secret_id_ttl=10m", "token_ttl=20m", "token_max_ttl=30m", "secret_id_num_uses=40");
    }

    /**
     * Prepares the Vault server for testing of the PKI auth backend (i.e. mounts the backend and populates test data).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupBackendPki() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_CERT_PEMFILE, rootToken);

        runCommand("vault", "mount", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "-path=pki", "pki");
        runCommand("vault", "mount", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "-path=other-pki", "pki");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "pki/root/generate/internal",
                   "common_name=myvault.com", "ttl=99h");
    }

    /**
     * Prepares the Vault server for testing of the TLS Certificate auth backend (i.e. mounts the backend and registers
     * the certificate and private key for client auth).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupBackendCert() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_CERT_PEMFILE, rootToken);

        runCommand("vault", "auth-enable", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "cert");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_CERT_PEMFILE, "auth/cert/certs/web", "display_name=web",
                   "policies=web,prod", "certificate=@" + CONTAINER_CLIENT_CERT_PEMFILE, "ttl=3600");
    }

    /**
     * <p>Constructs an instance of the Vault driver, providing maximum flexibility to control all options
     * explicitly.</p>
     *
     * <p>If <code>maxRetries</code> and <code>retryMillis</code> are BOTH null, then the <code>Vault</code>
     * instance will be constructed with retry logic disabled.  If one OR the other are null, the the class-level
     * default value will be used in place of the missing one.</p>
     *
     * @param config
     * @param maxRetries
     * @param retryMillis
     * @return
     */
    public Vault getVault(final VaultConfig config, final Integer maxRetries, final Integer retryMillis) {
        Vault vault = new Vault(config);
        if (maxRetries != null && retryMillis != null) {
            vault = vault.withRetries(maxRetries, retryMillis);
        } else if (maxRetries != null && retryMillis == null) {
            vault = vault.withRetries(maxRetries, RETRY_MILLIS);
        } else if (maxRetries == null && retryMillis != null) {
            vault = vault.withRetries(MAX_RETRIES, retryMillis);
        }
        return vault;
    }

    /**
     * Constructs an instance of the Vault driver, using sensible defaults.
     *
     * @return
     * @throws VaultException
     */
    public Vault getVault() throws VaultException {
        final VaultConfig config =
                new VaultConfig()
                        .address(getAddress())
                        .openTimeout(5)
                        .readTimeout(30)
                        .sslConfig(new SslConfig().pemFile(new File(CERT_PEMFILE)).build())
                        .build();
        return getVault(config, MAX_RETRIES, RETRY_MILLIS);
    }

    /**
     * Constructs an instance of the Vault driver with sensible defaults, configured to use the supplied token
     * for authentication.
     *
     * @param token
     * @return
     * @throws VaultException
     */
    public Vault getVault(final String token) throws VaultException {
        final VaultConfig config =
                new VaultConfig()
                        .address(getAddress())
                        .token(token)
                        .openTimeout(5)
                        .readTimeout(30)
                        .sslConfig(new SslConfig().pemFile(new File(CERT_PEMFILE)).build())
                        .build();
        return new Vault(config).withRetries(MAX_RETRIES, RETRY_MILLIS);
    }

    /**
     * Constructs an instance of the Vault driver with sensible defaults, configured to the use the root token
     * for authentication.
     *
     * @return
     * @throws VaultException
     */
    public Vault getRootVault() throws VaultException {
        return getVault(rootToken).withRetries(MAX_RETRIES, RETRY_MILLIS);
    }

    /**
     * Initial this class was launching a Docker container with bridged networking, and port 8200 on the host
     * machine mapped to whatever port Vault was actually using inside of the Docker container.  So this method
     * was necessary to dynamically build a URL string to that port.
     *
     * Once SSL support was added, there were problems with Vault's SSL certificate not recognizing the host
     * machine as a valid subject.  This could probably be overcome, by having the "createVaultCertAndKey()" method
     * above use "InetAddress.getLocalHost().getHostName()" to detect the host's hostname, and programmatically
     * add it to the cert's subject alt names.
     *
     * However, switching Docker to use "setNetworkMode=host" does away with the problem by making Docker's
     * view of the network identical to the host.  This would be considered insecure for production use, but
     * should be fine for a container that runs on a developer workstation or build server only for the duration
     * of this test suite.
     *
     * So at least for now, the original logic is commented out and the method returns a hardcoded string instead.jj
     *
     * @return The URL of the Vault instance
     */
    public String getAddress() {
//        return String.format("https://%s:%d", container.getContainerIpAddress(), container.getMappedPort(8200));
        return "https://127.0.0.1:8200";
    }

    /**
     * Runs the specified command from within the Docker container.
     *
     * @param command The command to run, broken up by whitespace
     *                (e.g. "vault mount -path=pki pki" becomes "vault", "mount", "-path=pki", "pki")
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private Container.ExecResult runCommand(final String... command) throws IOException, InterruptedException {
//        LOGGER.info("Command: {}", String.join(" ", command));
        final Container.ExecResult result = this.container.execInContainer(command);
//        LOGGER.info("Command stdout: {}", result.getStdout());
//        LOGGER.info("Command stderr: {}", result.getStderr());
        return result;
    }

    /**
     * <p>Called by the constructor method prior to configuring and launching the Vault container.  Uses Bouncy Castle
     * (https://www.bouncycastle.org) to programmatically generate a private key and X509 certificate for use by
     * the Vault server instance in accepting SSL connections.</p>
     *
     * <p>That the TestContainers library already includes Bouncy Castle as part of a shaded jar, so this class
     * is just importing that (note the renamed packages in the import statements).  If TestContainers for any
     * reason stops bundling Bouncy Castle in the future, then it will need to be separately added to this Gradle
     * project as a dependency with "testCompile" scope.</p>
     *
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws OperatorCreationException
     * @throws InvalidKeyException
     * @throws NoSuchProviderException
     * @throws SignatureException
     * @throws IOException
     */
    /*
    private void createVaultCertAndKey() throws NoSuchAlgorithmException, IOException, OperatorCreationException,
            CertificateException, InvalidKeyException, NoSuchProviderException, SignatureException {

        // Create directories and delete existing content as necessary, to write the cert and key to disk
        final File sslDir = new File(SSL_DIRECTORY);
        if (!sslDir.exists()) {
            if (!sslDir.mkdirs()) {
                LOGGER.error("Unable to create directory: " + SSL_DIRECTORY);
            }
        }
        final File rootCaPemfile = new File(CERT_PEMFILE);
        final File privateKeyFile = new File(PRIVATE_KEY_PEMFILE);
        if (rootCaPemfile.exists()) {
            if (!rootCaPemfile.delete()) {
                LOGGER.error("Unable to delete pre-existing file: " + CERT_PEMFILE);
            }
        }
        if (privateKeyFile.exists()) {
            if (!privateKeyFile.delete()) {
                LOGGER.error("Unable to delete pre-existing file: " + PRIVATE_KEY_PEMFILE);
            }
        }

        // Generate a certificate and private key for Vault, and write them to disk in PEM format.  Also store the
        // original X509Certificate object in a member variable, so it can later be used by "createClientCertAndKey()".
        final KeyPair keyPair = generateKeyPair();
        vaultCertificate = generateCert(keyPair, "C=AU, O=The Legion of the Bouncy Castle, OU=Vault Server Certificate, CN=localhost");
        writeCertToPem(vaultCertificate, CERT_PEMFILE);
        writePrivateKeyToPem(keyPair.getPrivate(), PRIVATE_KEY_PEMFILE);
    }
    */

    /**
     * <p>Constructs a Java truststore in JKS format, containing the Vault server certificate generated by
     * {@link this#createVaultCertAndKey()}, so that Vault clients configured with this JKS will trust that
     * certificate.</p>
     *
     * <p>Also constructs a JKS keystore, with a client certificate to use for authentication with Vault's TLS
     * Certificate auth backend.  Stores this cert as a PEM file as well, so that can be registered with Vault
     * as a recognized certificate in {@link this#setupBackendCert()}.</p>
     *
     * <p>Obviously, this method must be called AFTER {@link this#createVaultCertAndKey()}, and BEFORE
     * {@link this#setupBackendCert()}.</p>
     *
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    private void createClientCertAndKey() throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, OperatorCreationException, NoSuchProviderException, InvalidKeyException, SignatureException {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        final FileReader fileReader = new FileReader(CERT_PEMFILE);
        final PEMReader pemReader = new PEMReader(fileReader);
        vaultCertificate = (X509Certificate) pemReader.readObject();

        // Store the Vault's server certificate as a trusted cert in the truststore
        final KeyStore trustStore = KeyStore.getInstance("jks");
        trustStore.load(null);
        trustStore.setCertificateEntry("cert", vaultCertificate);
        try (final FileOutputStream keystoreOutputStream = new FileOutputStream(CLIENT_TRUSTSTORE)) {
            trustStore.store(keystoreOutputStream, "password".toCharArray());
        }

        // Generate a client certificate, and store it in a Java keystore
        final KeyPair keyPair = generateKeyPair();
        final X509Certificate clientCertificate =
                generateCert(keyPair, "C=AU, O=The Legion of the Bouncy Castle, OU=Client Certificate, CN=localhost");
        final KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null);
        keyStore.setKeyEntry("privatekey", keyPair.getPrivate(), "password".toCharArray(), new Certificate[]{clientCertificate});
        keyStore.setCertificateEntry("cert", clientCertificate);
        try (final FileOutputStream keystoreOutputStream = new FileOutputStream(CLIENT_KEYSTORE)) {
            keyStore.store(keystoreOutputStream, "password".toCharArray());
        }

        // Also write the client certificate to a PEM file, so it can be registered with Vault
        writeCertToPem(clientCertificate, CLIENT_CERT_PEMFILE);
        writePrivateKeyToPem(keyPair.getPrivate(), CLIENT_PRIVATE_KEY_PEMFILE);
    }

    /**
     * See https://www.cryptoworkshop.com/guide/, chapter 3
     *
     * @return A 4096-bit RSA keypair
     * @throws NoSuchAlgorithmException
     */
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
        keyPairGenerator.initialize(4096);
        return keyPairGenerator.genKeyPair();
    }

    /**
     * See http://www.programcreek.com/java-api-examples/index.php?api=org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
     *
     * @param keyPair The RSA keypair with which to generate the certificate
     * @param issuer The issuer (and subject) to use for the certificate
     * @return An X509 certificate
     * @throws IOException
     * @throws OperatorCreationException
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    private X509Certificate generateCert(final KeyPair keyPair, final String issuer) throws IOException, OperatorCreationException,
            CertificateException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException,
            SignatureException {
        final String subject = issuer;
        final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                new X500Name(issuer),
                BigInteger.ONE,
                new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30),
                new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
                new X500Name(subject),
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        final GeneralNames subjectAltNames = new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
        certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

        final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WithRSAEncryption");
        final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        final BcContentSignerBuilder signerBuilder = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
        final AsymmetricKeyParameter keyp = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        final ContentSigner signer = signerBuilder.build(keyp);
        final X509CertificateHolder x509CertificateHolder = certificateBuilder.build(signer);

        final X509Certificate certificate = new JcaX509CertificateConverter()
                .getCertificate(x509CertificateHolder);
        certificate.checkValidity(new Date());
        certificate.verify(keyPair.getPublic());
        return certificate;
    }

    /**
     * See https://stackoverflow.com/questions/3313020/write-x509-certificate-into-pem-formatted-string-in-java
     *
     * @param certificate An X509 certificate
     * @param filename The name (including path) of a file to which the certificate will be written in PEM format
     * @throws CertificateEncodingException
     * @throws FileNotFoundException
     */
    private void writeCertToPem(final X509Certificate certificate, final String filename)
            throws CertificateEncodingException, FileNotFoundException {
        final Base64.Encoder encoder = Base64.getEncoder();

        final String certHeader = "-----BEGIN CERTIFICATE-----\n";
        final String certFooter = "\n-----END CERTIFICATE-----";
        final byte[] certBytes = certificate.getEncoded();
        final String certContents = new String(encoder.encode(certBytes));
        final String certPem = certHeader + certContents + certFooter;
        try (final PrintWriter out = new PrintWriter(filename)){
            out.println(certPem);
        }
    }

    /**
     * See https://stackoverflow.com/questions/3313020/write-x509-certificate-into-pem-formatted-string-in-java
     *
     * @param key An RSA private key
     * @param filename The name (including path) of a file to which the private key will be written in PEM format
     * @throws FileNotFoundException
     */
    private void writePrivateKeyToPem(final PrivateKey key, final String filename) throws FileNotFoundException {
        final Base64.Encoder encoder = Base64.getEncoder();

        final String keyHeader = "-----BEGIN PRIVATE KEY-----\n";
        final String keyFooter = "\n-----END PRIVATE KEY-----";
        final byte[] keyBytes = key.getEncoded();
        final String keyContents = new String(encoder.encode(keyBytes));
        final String keyPem = keyHeader + keyContents + keyFooter;
        try (final PrintWriter out = new PrintWriter(filename)){
            out.println(keyPem);
        }
    }

}
