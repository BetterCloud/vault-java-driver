package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.rest.Rest;

public abstract class AbstractAPIClient {
    private final VaultConfig config;
    private Rest restClient;

    public AbstractAPIClient(final VaultConfig config) {
        this.config = config;
        this.restClient = new Rest()// NOPMD
            .keystore(config.getKeystore())
            .keystorePassword(config.getKeystorePassword())
            .truststore(config.getTruststore())
            .truststorePassword(config.getTruststorePassword())
            .connectTimeoutSeconds(config.getOpenTimeout())
            .readTimeoutSeconds(config.getReadTimeout())
            .sslVerification(config.isSslVerify())
            .sslPemUTF8(config.getSslPemUTF8());
    }

    public VaultConfig getConfig() {
        return this.config;
    }

    protected Rest getClient() {
        return restClient.clone();
    }

    protected void setClient(Rest client) {
        this.restClient = client;
    }
}
