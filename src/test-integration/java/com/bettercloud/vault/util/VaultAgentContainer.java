package com.bettercloud.vault.util;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.dockerjava.api.model.Capability;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import static org.junit.Assume.assumeTrue;
import static org.testcontainers.utility.MountableFile.forHostPath;

public class VaultAgentContainer extends GenericContainer<VaultAgentContainer> implements TestConstants, TestLifecycleAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultAgentContainer.class);

    /**
     * Establishes a running Docker container, hosting a Vault agent instance.
     */
    public VaultAgentContainer(
            Path roleId,
            Path secretId) {
        super("vault:1.2.1");
        this.withNetwork(CONTAINER_NETWORK)
                .withNetworkAliases("agent")
                .withClasspathResourceMapping("/agent.hcl", AGENT_CONFIG_FILE, BindMode.READ_ONLY)
                .withFileSystemBind(SSL_DIRECTORY, CONTAINER_SSL_DIRECTORY, BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(command -> command.withCapAdd(Capability.IPC_LOCK))
                .withCopyFileToContainer(forHostPath(roleId), "/home/vault/role_id")
                .withCopyFileToContainer(forHostPath(secretId), "/home/vault/secret_id")
                .withExposedPorts(8100)
                .withEnv("VAULT_CACERT", CONTAINER_CERT_PEMFILE)
                .withCommand(String.format("vault agent -config=%s", AGENT_CONFIG_FILE))
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .waitingFor(Wait.forLogMessage(".*renewed auth token.*", 1));
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
                        .build();
        return new Vault(config);
    }

    /**
     * The Docker container uses bridged networking.  Meaning that Vault listens on port 8200 inside the container,
     * but the tests running on the host machine cannot reach that port directly.  Instead, the Vault connection
     * config has to use a port that is mapped to the container's port 8200.  There is no telling what the mapped
     * port will be until runtime, so this method is necessary to build a Vault connection URL with the appropriate
     * values.
     *
     * @return The URL of the Vault instance
     */
    public String getAddress() {
        return String.format("http://%s:%d", getContainerIpAddress(), getMappedPort(8100));
    }

    @Override
    public void beforeTest(TestDescription description) {
        assumeTrue(DOCKER_AVAILABLE);
    }
}
