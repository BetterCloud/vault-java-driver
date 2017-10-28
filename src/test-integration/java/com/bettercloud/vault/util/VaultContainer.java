package com.bettercloud.vault.util;

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
import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.function.Consumer;

/** Sets up and exposes utilities for dealing with a Docker-hosted instance of Vault, for integration tests. */
public class VaultContainer implements TestRule, TestConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultContainer.class);

    private final GenericContainer container;
    private String rootToken;
    private String unsealKey;

    /**
     * Establishes a running Docker container, hosting a Vault server instance.
     *
     * TODO: Move SSL generation to a separate Docker container, run only once per test suite.
     *
     * Right now, the "startup.sh" script uses OpenSSL commands to generate all of the private keys
     * and certificates needed by the integration test suite... depositing them in the `[PROJECT_ROOT]/ssl`
     * subdirectory, so they'll also be accessible by the Java code.
     *
     * This is an expensive operation.  Since the official Vault docker container (based on Alpine Linux)
     * doesn't come with OpenSSL installed, the startup script has to download and install that before
     * doing anything else.  This is about an 8 MB downloaded, and sluggish install process, and it is
     * repeated separately for EVERY class in the test suite!
     *
     * It would be better to have a static method, with a static state variable to detect whether it's
     * already been invoked once.  This method would launch a plain Alpine Linux container and install
     * OpenSSL (or maybe just use some other lightweight distro with OpenSSL already installed).  It
     * would do the SSL generation as a one-time operation per test suite invocation.  Then the regular
     * Vault containers could just mount that `/ssl` subdirectory and find the artifacts already generated.
     */
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
                        // All of the tests in this integration test suite use HTTPS connections.  However, Vault
                        // is configured to run a plain HTTP listener on port 8280, purely for purposes of detecting
                        // when the Docker container is fully ready.
                        //
                        // Unfortunately, we can't use HTTPS at this point in the flow.  Because that would require
                        // configuring SSL to trust the self-signed cert that's generated inside of the Docker
                        // container.  A chicken-and-egg problem, as we need to wait for the container to be fully
                        // ready before we access that cert.
                        new HttpWaitStrategy() {
                            @Override
                            protected Integer getLivenessCheckPort() {
                                return 8280;
                            }
                        }
                        .forPath("/v1/sys/seal-status")
                        .forStatusCode(HttpURLConnection.HTTP_BAD_REQUEST) // The expected response when "vault init" has not yet run
                );
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
     * use "InetAddress.getLocalHost().getHostName()" to detect the host's hostname, and programmatically
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
        LOGGER.info("Command: {}", String.join(" ", command));
        final Container.ExecResult result = this.container.execInContainer(command);
        final String out = result.getStdout();
        final String err = result.getStderr();
        if (out != null && !out.isEmpty()) {
            LOGGER.info("Command stdout: {}", result.getStdout());
        }
        if (err != null && !err.isEmpty()) {
            LOGGER.info("Command stderr: {}", result.getStderr());
        }
        return result;
    }

}
