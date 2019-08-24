package com.bettercloud.vault.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

public class DbContainer extends GenericContainer<DbContainer> implements TestConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbContainer.class);

    public static final String hostname = "postgres";

    public DbContainer() {
        super("postgres:11.3-alpine");
        this.withNetwork(CONTAINER_NETWORK)
                .withNetworkAliases(hostname)
                .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
                .withEnv("POSTGRES_USER", POSTGRES_USER)
                .withExposedPorts(5432)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .waitingFor(new HostPortWaitStrategy());
    }
}
