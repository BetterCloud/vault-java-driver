package com.bettercloud.vault.api;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Capability;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.shaded.org.bouncycastle.asn1.x500.X500Name;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.Extension;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralNames;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.testcontainers.shaded.org.bouncycastle.cert.X509CertificateHolder;
import org.testcontainers.shaded.org.bouncycastle.cert.X509v1CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.cert.X509v3CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.testcontainers.shaded.org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testcontainers.shaded.org.bouncycastle.operator.ContentSigner;
import org.testcontainers.shaded.org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.testcontainers.shaded.org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.testcontainers.shaded.org.bouncycastle.operator.OperatorCreationException;
import org.testcontainers.shaded.org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.testcontainers.shaded.org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.testcontainers.shaded.org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Sets up and exposes utilities for dealing with a Docker-hosted instance of Vault, for integration tests.
 */
public class VaultContainer implements TestRule {

    public static final String APP_ID = "fake_app";
    public static final String USER_ID = "fake_user";
    public static final String PASSWORD = "fake_password";

    private static final String CURRENT_WORKING_DIRECTORY = System.getProperty("user.dir");
    private static final String SSL_DIRECTORY = CURRENT_WORKING_DIRECTORY + File.separator + "ssl";
    private static final String ROOT_CA_PEMFILE = SSL_DIRECTORY + File.separator + "root-ca.crt";
    private static final String PRIVATE_KEY_PEMFILE = SSL_DIRECTORY + File.separator + "private.key";

    private static final String CONTAINER_CONFIG_FILE = "/vault/config/config.json";
    private static final String CONTAINER_SSL_DIRECTORY = "/vault/config/ssl";
    private static final String CONTAINER_ROOT_CA_PEMFILE = CONTAINER_SSL_DIRECTORY + File.separator + "root-ca.crt";

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_MILLIS = 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultContainer.class);

    private final GenericContainer container;
    private String rootToken;
    private String unsealKey;

    /**
     * Establishes a running Docker container, hosting a Vault server instance.
     */
    public VaultContainer() {

        // Generate SSL key and cert
        try {
            createCACertAndKey();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start Docker container
        this.container = new GenericContainer("vault:0.7.3")
                .withClasspathResourceMapping("/config.json", CONTAINER_CONFIG_FILE, BindMode.READ_ONLY)
                .withFileSystemBind(SSL_DIRECTORY, CONTAINER_SSL_DIRECTORY, BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
                    // TODO: Why does the compiler freak out when this anonymous class is converted to a lambda?
                    @Override
                    public void accept(final CreateContainerCmd createContainerCmd) {
                        createContainerCmd.withCapAdd(Capability.IPC_LOCK);
                    }
                })
                .withNetworkMode("host")  // .withExposedPorts(8200)
                .withCommand("vault", "server", "-config", CONTAINER_CONFIG_FILE);
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
        final Container.ExecResult initResult = runCommand("vault",  "init", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "-key-shares=1", "-key-threshold=1");
        final String[] initLines = initResult.getStdout().split(System.lineSeparator());
        this.unsealKey = initLines[0].replace("Unseal Key 1: ", "");
        this.rootToken = initLines[1].replace("Initial Root Token: ", "");

        System.out.println("Root token: " + rootToken);

        // Unseal the Vault server
        runCommand("vault",  "unseal", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, unsealKey);
    }

    /**
     * Prepares the Vault server for testing of the AppID auth backend (i.e. mounts the backend and populates test
     * data).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupAppIdBackend() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, rootToken);

        runCommand("vault", "auth-enable", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "app-id");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "auth/app-id/map/app-id/" + APP_ID, "display_name=" + APP_ID);
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "auth/app-id/map/user-id/" + USER_ID, "value=" + APP_ID);
    }

    /**
     * Prepares the Vault server for testing of the Username and Password auth backend (i.e. mounts the backend and
     * populates test data).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupUserPassBackend() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, rootToken);

        runCommand("vault", "auth-enable", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "userpass");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "auth/userpass/users/" + USER_ID, "password=" + PASSWORD);
    }

    /**
     * Prepares the Vault server for testing of the AppRole auth backend (i.e. mounts the backend and populates test
     * data).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupAppRoleBackend() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, rootToken);

        runCommand("vault", "auth-enable", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "approle");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "auth/approle/role/testrole",
                   "secret_id_ttl=10m", "token_ttl=20m", "token_max_ttl=30m", "secret_id_num_uses=40");
    }

    /**
     * Prepares the Vault server for testing of the PKI auth backend (i.e. mounts the backend and populates test data).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupPkiBackend() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, rootToken);

        runCommand("vault", "mount", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "-path=pki", "pki");
        runCommand("vault", "mount", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "-path=other-pki", "pki");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "pki/root/generate/internal",
                   "common_name=myvault.com", "ttl=99h");
    }

    /**
     * Prepares the Vault server for testing of the TLS Certificate auth backend (i.e. mounts the backend and registers
     * the certificate and private key for client auth).
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupCertBackend() throws IOException, InterruptedException {
        runCommand("vault", "auth", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, rootToken);

        runCommand("vault", "auth-enable", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "cert");
        runCommand("vault", "write", "-ca-cert=" + CONTAINER_ROOT_CA_PEMFILE, "auth/cert/certs/web", "display_name=web",
                   "policies=web,prod", "certificate=@" + CONTAINER_ROOT_CA_PEMFILE, "ttl=3600");
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
//                        .verify(false)
                        .sslConfig(new SslConfig().pemFile(new File(ROOT_CA_PEMFILE)).clientPemFile(new File(ROOT_CA_PEMFILE)).clientKeyPemFile(new File(PRIVATE_KEY_PEMFILE)).build())
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
//                        .verify(false)
                        .sslConfig(new SslConfig().pemFile(new File(ROOT_CA_PEMFILE)).clientPemFile(new File(ROOT_CA_PEMFILE)).clientKeyPemFile(new File(PRIVATE_KEY_PEMFILE)).build())
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
     * machine as a valid subject.  This could probably be overcome, by having the "createCACertAndKey()" method
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
    private String getAddress() {
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
        LOGGER.info("Command: {}", String.join(" ", command));
        final Container.ExecResult result = this.container.execInContainer(command);
        LOGGER.info("Command stdout: {}", result.getStdout());
        LOGGER.info("Command stderr: {}", result.getStderr());
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
    private void createCACertAndKey() throws NoSuchAlgorithmException, IOException, OperatorCreationException,
            CertificateException, InvalidKeyException, NoSuchProviderException, SignatureException {

        // STEP 1:  Create a public/private keypair
        // (see https://www.cryptoworkshop.com/guide/, chapter 3)

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
        keyPairGenerator.initialize(4096);
        final KeyPair keyPair = keyPairGenerator.genKeyPair();
        final PublicKey publicKey = keyPair.getPublic();
        final PrivateKey privateKey = keyPair.getPrivate();


        // STEP 2:  Use the keypair to create an x509 root CA certificate
        // (see http://www.programcreek.com/java-api-examples/index.php?api=org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder)

        final String issuer = "C=AU, O=The Legion of the Bouncy Castle, OU=Bouncy Primary Certificate, CN=localhost";
        final String subject = issuer;
        final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                new X500Name(issuer),
                BigInteger.ONE,
                new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30),
                new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
                new X500Name(subject),
                SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())
        );

        final GeneralNames subjectAltNames = new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
        certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

        final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WithRSAEncryption");
        final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        final BcContentSignerBuilder signerBuilder = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
        final AsymmetricKeyParameter keyp = PrivateKeyFactory.createKey(privateKey.getEncoded());
        final ContentSigner signer = signerBuilder.build(keyp);
        final X509CertificateHolder x509CertificateHolder = certificateBuilder.build(signer);

        final X509Certificate x509Certificate = new JcaX509CertificateConverter()
                .getCertificate(x509CertificateHolder);
        x509Certificate.checkValidity(new Date());
        x509Certificate.verify(publicKey);


        // STEP 3:  Write the x509 root CA certificate and private key to PEM files, for use by Vault
        // (see: https://stackoverflow.com/questions/3313020/write-x509-certificate-into-pem-formatted-string-in-java)

        final File sslDir = new File(SSL_DIRECTORY);
        if (!sslDir.exists()) {
            if (!sslDir.mkdirs()) {
                LOGGER.error("Unable to create directory: " + SSL_DIRECTORY);
            }
        }
        final File rootCaPemfile = new File(ROOT_CA_PEMFILE);
        final File privateKeyFile = new File(PRIVATE_KEY_PEMFILE);
        if (rootCaPemfile.exists()) {
            if (!rootCaPemfile.delete()) {
                LOGGER.error("Unable to delete pre-existing file: " + ROOT_CA_PEMFILE);
            }
        }
        if (privateKeyFile.exists()) {
            if (!privateKeyFile.delete()) {
                LOGGER.error("Unable to delete pre-existing file: " + PRIVATE_KEY_PEMFILE);
            }
        }

        final Base64.Encoder encoder = Base64.getEncoder();

        final String certHeader = "-----BEGIN CERTIFICATE-----\n";
        final String certFooter = "\n-----END CERTIFICATE-----";
        final byte[] certBytes = x509Certificate.getEncoded();
        final String certContents = new String(encoder.encode(certBytes));
        final String certPem = certHeader + certContents + certFooter;
        try (final PrintWriter out = new PrintWriter(ROOT_CA_PEMFILE)  ){
            out.println(certPem);
        }

        final String keyHeader = "-----BEGIN PRIVATE KEY-----\n";
        final String keyFooter = "\n-----END PRIVATE KEY-----";
        final byte[] keyBytes = privateKey.getEncoded();
        final String keyContents = new String(encoder.encode(keyBytes));
        final String keyPem = keyHeader + keyContents + keyFooter;
        try (final PrintWriter out = new PrintWriter(PRIVATE_KEY_PEMFILE)  ){
            out.println(keyPem);
        }
    }

    /**
     * This is code for programmatically generating a private key and X509 certificate in Java-friendly JKS format.
     * The Vault server instance running inside of the Docker container doesn't need this, as Vault natively uses
     * files in PEM format.  However, this is left in here for now (unused) in case we decide to add JKS support up
     * at the client driver level.
     *
     * If so, then this logic will probably move to the {@link this#createCACertAndKey()} method... which will then
     * store its cert and key in both formats.  In either case, this method should probably be removed before the
     * the next release of this library to Maven Central.
     */
    @Deprecated
    private File setupCertBackendOld() throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, KeyStoreException, IOException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());
        keyPairGenerator.initialize(4096);
        final KeyPair keyPair = keyPairGenerator.genKeyPair();
        final PrivateKey privateKey = keyPair.getPrivate();

        final X509v1CertificateBuilder certBldr = new JcaX509v1CertificateBuilder(
                new X500Name("CN=Test Root Certificate"),
                BigInteger.valueOf(1),
                new Date(System.currentTimeMillis()),
                new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)),
                new X500Name("CN=Test Root Certificate"),
                keyPair.getPublic()
        );
        final ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider(new BouncyCastleProvider())
                                                                               .build(keyPair.getPrivate());
        final X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                                                                                 .getCertificate(certBldr.build(signer));

        final KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null);
        final Certificate[] chain = { x509Certificate };
        keyStore.setKeyEntry("privateKey", privateKey, "testpassword".toCharArray(), chain);
        try (final FileOutputStream keystoreOutputStream = new FileOutputStream("keystore.jks")) {
            keyStore.store(keystoreOutputStream, "testpassword".toCharArray());
        }

        return new File("keystore.jks");
    }

}
