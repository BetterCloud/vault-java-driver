package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;

/**
 * @author Luis Casillas
 */
public class VaultContainer implements TestRule {
    private static final Logger log = LoggerFactory.getLogger(VaultContainer.class);

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_MILLIS = 1000;

    private final String rootToken;
    private final GenericContainer vault;

    public VaultContainer(String rootToken) {
        this.rootToken = rootToken;
        this.vault = new GenericContainer("vault:v0.6.0")
                .withExposedPorts(8200)
                .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
                .withEnv("VAULT_DEV_ROOT_TOKEN_ID", rootToken)
                .withEnv("VAULT_ADDR", "http://127.0.0.1:8200");
    }


    @Override
    public Statement apply(Statement base, Description description) {
        return vault.apply(base, description);
    }


    public void followOutput() {
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
        vault.followOutput(logConsumer);
    }

    public Vault getRootVault() throws VaultException {
        return getVault(rootToken).withRetries(MAX_RETRIES, RETRY_MILLIS);
    }

    public Vault getVault() throws VaultException {
        VaultConfig config =
                new VaultConfig()
                        .address(getAddress())
                        .openTimeout(5)
                        .readTimeout(30)
                        .sslVerify(false)
                        .build();
        return new Vault(config).withRetries(MAX_RETRIES, RETRY_MILLIS);
    }

    public Vault getVault(String token) throws VaultException {
        VaultConfig config =
                new VaultConfig()
                        .address(getAddress())
                        .token(token)
                        .openTimeout(5)
                        .readTimeout(30)
                        .sslVerify(false)
                        .build();
        return new Vault(config).withRetries(MAX_RETRIES, RETRY_MILLIS);
    }

    public String getAddress() {
        // FIXME: If this address ends with a slash (`"http://%s:%d/"`), tests fail!
        return String.format("http://%s:%d", vault.getContainerIpAddress(), vault.getMappedPort(8200));
    }

    public Container.ExecResult runCommand(String... command) throws IOException, InterruptedException {
        Container.ExecResult result = vault.execInContainer(command);
        log.debug("Command stdout: {}", result.getStdout());
        log.debug("Command stderr: {}", result.getStderr());
        return result;
    }

    public void createAuthExample(String appId, String userId, String password)
            throws IOException, InterruptedException {
        runCommand("vault", "auth-enable", "app-id");
        runCommand("vault", "write", "auth/app-id/map/app-id/" + appId, "value=root", "display_name=" + appId);
        runCommand("vault", "write", "auth/app-id/map/user-id/" + userId, "value=" + appId);

        runCommand("vault", "auth-enable", "userpass");
        runCommand("vault", "write", "auth/userpass/users/" + userId, "password=" + password, "policies=root");
    }

    public void createPkiExample() throws IOException, InterruptedException {
        runCommand("vault", "mount", "-path=pki", "pki");
        runCommand("vault", "write", "pki/root/generate/internal", "common_name=myvault.com", "ttl=99h");
    }
}
