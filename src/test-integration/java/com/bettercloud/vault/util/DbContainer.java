package com.bettercloud.vault.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.HostPortWaitStrategy;

import java.io.IOException;

public class DbContainer implements TestRule, TestConstants {

    private final GenericContainer container;

    public DbContainer() {
        container = new GenericContainer("postgres:11.3-alpine")
                .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
                .withEnv("POSTGRES_USER", POSTGRES_USER)
                .withExposedPorts(5432)
                .waitingFor(new HostPortWaitStrategy());
    }

    public String getDbContainerIp() throws IOException, InterruptedException {
        Container.ExecResult ip = container.execInContainer("hostname", "-i");
        return ip.getStdout().replace("\n", "");

        //return container.getContainerIpAddress();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return container.apply(base, description);
    }
}
