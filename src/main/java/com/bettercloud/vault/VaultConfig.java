package com.bettercloud.vault;

/**
 * <p>A container for the configuration settings needed to initialize a <code>Vault</code> driver instance.</p>
 *
 * <p>There are two ways to create and setup a <code>VaultConfig</code> instance.  The full-featured approach
 * uses a builder pattern, calling setter methods for each value and then terminating with a call to <code>build()</code>:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final VaultConfig config = new VaultConfig()
 *                              .address("http://127.0.0.1:8200")
 *                              .token("eace6676-4d78-c687-4e54-03cad00e3abf")
 *                              .sslVerify(true)
 *                              .timeout(30)
 *                              .build();
 * }</pre>
 * </blockquote>
 *
 * <p>If the only values that you need to set are <code>address</code> and <code>token</code>, then as a
 * shortcut there is also a constructor method taking those two values:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final VaultConfig config = new VaultConfig("http://127.0.0.1:8200", "eace6676-4d78-c687-4e54-03cad00e3abf");
 * }</pre>
 * </blockquote>
 *
 * <p>Note that when using the shorthand convenience constructor, you should NOT set additional properties on the
 * same instance afterward.</p>
 */
public final class VaultConfig {

    /**
     * <p>The code used to load environment variables is encapsulated within an inner class,
     * so that a mock version of that environment loader can be used by unit tests.</p>
     */
    protected static class EnvironmentLoader {
        public String loadVariable(final String name) {
            return System.getenv(name);
        }
    }

    private EnvironmentLoader environmentLoader;
    private String address;
    private String token;
    private String proxyAddress;
    private Integer proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private String sslPemFile;
    private Boolean sslVerify;
    private Integer timeout;
    private Integer sslTimeout;
    private Integer openTimeout;
    private Integer readTimeout;

    /**
     * <p>Default constructor.  Should be used in conjunction with the builder pattern, calling additional
     * property setter methods and ultimately finishing with a call to <code>build()</code>.</p>
     *
     * <p>Note that when using this builder pattern approach, you must either set <code>address</code>
     * and <code>token</code> explicitly, or else have them available as runtime environment variables.</p>
     */
    public VaultConfig() {
    }

    /**
     * <p>A convenience constructor, for quickly creating a <code>VaultConfig</code> instance with its
     * <code>address</code> and <code>token</code> fields populated.</p>
     *
     * <p>Although <code>address</code> and <code>token</code> are the only two properties explicitly
     * passed, the constructor will still look to the runtime environment variables to populate any
     * other fields when values are present.</p>
     *
     * <p>When using this approach to creating a <code>VaultConfig</code> instance, you should NOT
     * make additional setter method calls after construction.  If you need other properties set
     * explicitly, then use the builder pattern approach.</p>
     *
     * @param address The URL of the target Vault server
     * @param token The access token to enable Vault access
     * @throws VaultException
     */
    public VaultConfig(final String address, final String token) throws VaultException {
        this(address, token, new EnvironmentLoader());
    }

    /**
     * An overloaded version of the normal convenience constructor, used by unit tests to inject
     * a mock environment variable loader and validate that loading logic.
     *
     * @param address The URL of the target Vault server
     * @param token The access token to enable Vault access
     * @param environmentLoader A (mock) environment loader implementation
     * @throws VaultException
     */
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

    public VaultConfig proxyPort(final Integer proxyPort) {
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
        if (this.proxyAddress == null && environmentLoader.loadVariable("VAULT_PROXY_ADDRESS") != null) {
            this.proxyAddress = environmentLoader.loadVariable("VAULT_PROXY_ADDRESS");
        }
        if (this.proxyPort== null && environmentLoader.loadVariable("VAULT_PROXY_PORT") != null) {
            try {
                this.proxyPort = Integer.valueOf(environmentLoader.loadVariable("VAULT_PROXY_PORT"));
            } catch (NumberFormatException e) {
                System.err.printf("The \"VAULT_PROXY_PORT\" environment variable contains value \"%s\", which cannot be parsed as an integer port number.\n",
                        environmentLoader.loadVariable("VAULT_PROXY_PORT"));
            }
        }
        if (this.proxyUsername == null && environmentLoader.loadVariable("VAULT_PROXY_USERNAME") != null) {
            this.proxyUsername = environmentLoader.loadVariable("VAULT_PROXY_USERNAME");
        }
        if (this.proxyPassword == null && environmentLoader.loadVariable("VAULT_PROXY_PASSWORD") != null) {
            this.proxyPassword = environmentLoader.loadVariable("VAULT_PROXY_PASSWORD");
        }
        if (this.sslPemFile == null && environmentLoader.loadVariable("VAULT_SSL_CERT") != null) {
            this.sslPemFile = environmentLoader.loadVariable("VAULT_SSL_CERT");
        }
        if (this.sslVerify == null && environmentLoader.loadVariable("VAULT_SSL_VERIFY") != null) {
            this.sslVerify = Boolean.valueOf(environmentLoader.loadVariable("VAULT_SSL_VERIFY"));
        }
        if (this.timeout == null && environmentLoader.loadVariable("VAULT_TIMEOUT") != null) {
            try {
                this.timeout = Integer.valueOf(environmentLoader.loadVariable("VAULT_TIMEOUT"));
            } catch (NumberFormatException e) {
                System.err.printf("The \"VAULT_TIMEOUT\" environment variable contains value \"%s\", which cannot be parsed as an integer timeout period.\n",
                        environmentLoader.loadVariable("VAULT_TIMEOUT"));
            }
        }
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

    public Integer getProxyPort() {
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

    public Boolean isSslVerify() {
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
