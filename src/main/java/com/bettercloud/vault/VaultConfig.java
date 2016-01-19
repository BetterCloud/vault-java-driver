package com.bettercloud.vault;

public final class VaultConfig {

    protected static class EnvironmentLoader {
        public String loadVariable(final String name) {
            return System.getenv(name);
        }
    }

    private EnvironmentLoader environmentLoader;
    private String address;
    private String token;
    private String proxyAddress;
    private String proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private String sslPemFile;
    private Boolean sslVerify;
    private Integer timeout;
    private Integer sslTimeout;
    private Integer openTimeout;
    private Integer readTimeout;

    public VaultConfig() {
    }

    public VaultConfig(final String address, final String token) throws VaultException {
        this(address, token, new EnvironmentLoader());
    }

    protected VaultConfig(final String address, final String token, final EnvironmentLoader environmentLoader) throws VaultException {
        this.address = address;
        this.token = token;
        this.environmentLoader = environmentLoader;
        build();
    }

    protected VaultConfig environmentLoader(final EnvironmentLoader environmentLoader) {
        this.environmentLoader = environmentLoader;
        return this;
    }

    public VaultConfig address(final String address) {
        this.address = address;
        return this;
    }

    public VaultConfig token(final String token) {
        this.token = token;
        return this;
    }

    public VaultConfig proxyAddress(final String proxyAddress) {
        this.proxyAddress = proxyAddress;
        return this;
    }

    public VaultConfig proxyPort(final String proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public VaultConfig proxyUsername(final String proxyUsername) {
        this.proxyUsername = proxyUsername;
        return this;
    }

    public VaultConfig proxyPassword(final String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    public VaultConfig sslPemFile(final String sslPemFile) {
        this.sslPemFile = sslPemFile;
        return this;
    }

    public VaultConfig sslVerify(final Boolean sslVerify) {
        this.sslVerify = sslVerify;
        return this;
    }

    public VaultConfig timeout(final Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    public VaultConfig sslTimeout(final Integer sslTimeout) {
        this.sslTimeout = sslTimeout;
        return this;
    }

    public VaultConfig openTimeout(final Integer openTimeout) {
        this.openTimeout = openTimeout;
        return this;
    }

    public VaultConfig readTimeout(final Integer readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }


    public VaultConfig build() throws VaultException {
        if (this.environmentLoader == null) {
            this.environmentLoader = new EnvironmentLoader();
        }
        if (this.address == null) {
            final String addressFromEnv = environmentLoader.loadVariable("VAULT_ADDR");
            if (addressFromEnv != null) {
                this.address = addressFromEnv;
            } else {
                throw new VaultException("No address is set");
            }
        }
        if (this.token == null) {
            final String tokenFromEnv = environmentLoader.loadVariable("VAULT_TOKEN");
            if (tokenFromEnv != null) {
                this.token = tokenFromEnv;
            } else {
                throw new VaultException("No token is set");
            }
        }

        // TODO: Check the environment variables to populate null VAULT_PROXY_ADDRESS
        // TODO: Check the environment variables to populate null VAULT_PROXY_PORT
        // TODO: Check the environment variables to populate null VAULT_PROXY_USERNAME
        // TODO: Check the environment variables to populate null VAULT_PROXY_PASSWORD
        // TODO: Check the environment variables to populate null VAULT_SSL_CERT
        // TODO: Check the environment variables to populate null VAULT_SSL_VERIFY
        // TODO: Check the environment variables to populate null VAULT_TIMEOUT

        return this;
    }

    public String getAddress() {
        return address;
    }

    public String getToken() {
        return token;
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public String getSslPemFile() {
        return sslPemFile;
    }

    public Boolean getSslVerify() {
        return sslVerify;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public Integer getSslTimeout() {
        return sslTimeout;
    }

    public Integer getOpenTimeout() {
        return openTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

}
