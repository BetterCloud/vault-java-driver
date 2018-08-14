package com.bettercloud.vault;

import lombok.Getter;

import java.io.Serializable;

/**
 * <p>A container for the configuration settings needed to initialize a <code>Vault</code> driver instance.</p>
 *
 * <p>Construct instances of this class using a builder pattern, calling setter methods for each value and then
 * terminating with a call to build():</p>
 *
 * <blockquote>
 * <pre>{@code
 * final VaultConfig config = new VaultConfig()
 *                              .address("http://127.0.0.1:8200")
 *                              .token("eace6676-4d78-c687-4e54-03cad00e3abf")
 *                              .sslConfig(new SslConfig().verify(false).build())
 *                              .timeout(30)
 *                              .build();
 * }</pre>
 * </blockquote>
 *
 * @see SslConfig
 */
public class VaultConfig implements Serializable {

    protected static final String VAULT_TOKEN = "VAULT_TOKEN";
    private static final String VAULT_ADDR = "VAULT_ADDR";
    private static final String VAULT_OPEN_TIMEOUT = "VAULT_OPEN_TIMEOUT";
    private static final String VAULT_READ_TIMEOUT = "VAULT_READ_TIMEOUT";

    @Getter private String address;
    @Getter private String token;
    @Getter private SslConfig sslConfig;
    @Getter private Integer openTimeout;
    @Getter private Integer readTimeout;
    @Getter private int maxRetries;
    @Getter private int retryIntervalMilliseconds;
    private EnvironmentLoader environmentLoader;

    /**
     * <p>The code used to load environment variables is encapsulated here, so that a mock version of that environment
     * loader can be used by unit tests.</p>
     *
     * <p>This method is primarily intended for use by unit tests, to inject a mock environment variable when
     * constructing a <code>VaultConfig</code> instance using the builder pattern approach rather than the convenience
     * constructor.  This method's access level was therefore originally set to <code>protected</code>, but was bumped
     * up to <code>public</code> due to community request for the ability to disable environment loading altogether
     * (see https://github.com/BetterCloud/vault-java-driver/issues/77).
     *
     * Note that if you do override this, however, then obviously all of the environment checking discussed in the
     * documentation becomes disabled.
     *
     * @param environmentLoader An environment variable loader implementation (presumably a mock)
     * @return This object, with environmentLoader populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig environmentLoader(final EnvironmentLoader environmentLoader) {
        this.environmentLoader = environmentLoader;
        return this;
    }

    /**
     * <p>Sets the address (URL) of the Vault server instance to which API calls should be sent.
     * E.g. <code>http://127.0.0.1:8200</code>.</p>
     *
     * <p>If no address is explicitly set, the object will look to the <code>VAULT_ADDR</code> environment variable.</p>
     *
     * <p><code>address</code> is required for the Vault driver to function.  If you do not supply it explicitly AND no
     * environment variable value is found, then initialization of the <code>VaultConfig</code> object will fail.</p>
     *
     * @param address The Vault server base URL
     * @return This object, with address populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig address(final String address) {
        this.address = address.trim();
        if (this.address.endsWith("/")) {
            this.address = this.address.substring(0, this.address.length() - 1);
        }
        return this;
    }

    /**
     * <p>Sets the token used to access Vault.</p>
     *
     * <p>If no token is explicitly set, then the object will look to the <code>VAULT_TOKEN</code> environment
     * variable.</p>
     *
     * <p>There are some cases where you might want to instantiate a <code>VaultConfig</code> object without a token
     * (e.g. you plan to retrieve a token programmatically, with a call to the "userpass" auth backend, and populate
     * it prior to making any other API calls).</p>
     *
     * @param token The token to use for accessing Vault
     * @return This object, with token populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig token(final String token) {
        this.token = token;
        return this;
    }

    /**
     * <p>A container for SSL-related configuration options (e.g. certificates).</p>
     *
     * <p>Although typically necessary in most production environments, this is not strictly required (e.g. if your
     * Vault server address begins with "http://" instead of "https://", then any SSL config will be ignored).
     * However, if your Vault server uses HTTPS, and you wish to skip SSL certificate verification (NOT RECOMMENDED
     * FOR PRODUCTION!), then you must supply an <code>SslConfig</code> object with {@link SslConfig#verify(Boolean)}
     * explicitly set to <code>false</code>.</p>
     *
     * @param sslConfig SSL-related configuration options
     * @return This object, with SSL configuration options populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig sslConfig(final SslConfig sslConfig) {
        this.sslConfig = sslConfig;
        return this;
    }

    /**
     * <p>The number of seconds to wait before giving up on establishing an HTTP(S) connection to the Vault server.</p>
     *
     * <p>If no openTimeout is explicitly set, then the object will look to the <code>VAULT_OPEN_TIMEOUT</code>
     * environment variable.</p>
     *
     * @param openTimeout Number of seconds to wait for an HTTP(S) connection to successfully establish
     * @return This object, with openTimeout populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig openTimeout(final Integer openTimeout) {
        this.openTimeout = openTimeout;
        return this;
    }

    /**
     * <p>After an HTTP(S) connection has already been established, this is the number of seconds to wait for all
     * data to finish downloading.</p>
     *
     * <p>If no readTimeout is explicitly set, then the object will look to the <code>VAULT_READ_TIMEOUT</code>
     * environment variable.</p>
     *
     * @param readTimeout Number of seconds to wait for all data to be retrieved from an established HTTP(S) connection
     * @return This object, with readTimeout populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig readTimeout(final Integer readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * <p>Sets the maximum number of times that an API operation will retry upon failure.</p>
     *
     * <p>This method is not meant to be called from application-level code outside of this package (hence
     * the <code>protected</code> access level.  It is meant to be invoked via <code>Vault.withRetries()</code>
     * in a builder pattern DSL-style.</p>
     *
     * @param maxRetries The number of times that API operations will be retried when a failure occurs.
     */
    protected void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * <p>Sets the period of time (in milliseconds) that the driver will wait in between retry attempts for a
     * failing API operation.</p>
     *
     * <p>This method is not meant to be called from application-level code outside of this package (hence
     * the <code>protected</code> access level.  It is meant to be invoked via <code>Vault.withRetries()</code>
     * in a builder pattern DSL-style.</p>
     *
     * @param retryIntervalMilliseconds The number of milliseconds that the driver will wait in between retries.
     */
    protected void setRetryIntervalMilliseconds(final int retryIntervalMilliseconds) {
        this.retryIntervalMilliseconds = retryIntervalMilliseconds;
    }


    /**
     * <p>This is the terminating method in the builder pattern.  The method that validates all of the fields that
     * has been set already, uses environment variables when available to populate any unset fields, and returns
     * a <code>VaultConfig</code> object that is ready for use.</p>
     *
     * @return This object, with all available config options parsed and loaded
     * @throws VaultException If the <code>address</code> field was left unset, and there is no <code>VAULT_ADDR</code> environment variable value with which to populate it.
     */
    public VaultConfig build() throws VaultException {
        if (this.environmentLoader == null) {
            this.environmentLoader = new EnvironmentLoader();
        }
        if (this.address == null) {
            final String addressFromEnv = environmentLoader.loadVariable(VAULT_ADDR);
            if (addressFromEnv != null) {
                this.address = addressFromEnv;
            } else {
                throw new VaultException("No address is set");
            }
        }
        if (this.token == null && environmentLoader.loadVariable(VAULT_TOKEN) != null) {
            this.token = environmentLoader.loadVariable(VAULT_TOKEN);
        }
        if (this.openTimeout == null && environmentLoader.loadVariable(VAULT_OPEN_TIMEOUT) != null) {
            try {
                this.openTimeout = Integer.valueOf(environmentLoader.loadVariable(VAULT_OPEN_TIMEOUT));
            } catch (NumberFormatException e) {
                System.err.printf("The " + VAULT_OPEN_TIMEOUT + " environment variable contains value \"%s\", which cannot be parsed as an integer timeout period.%n",
                        environmentLoader.loadVariable(VAULT_OPEN_TIMEOUT));
            }
        }
        if (this.readTimeout == null && environmentLoader.loadVariable(VAULT_READ_TIMEOUT) != null) {
            try {
                this.readTimeout = Integer.valueOf(environmentLoader.loadVariable(VAULT_READ_TIMEOUT));
            } catch (NumberFormatException e) {
                System.err.printf("The " + VAULT_READ_TIMEOUT + " environment variable contains value \"%s\", which cannot be parsed as an integer timeout period.%n",
                        environmentLoader.loadVariable(VAULT_READ_TIMEOUT));
            }
        }
        if (this.sslConfig == null) {
            this.sslConfig = new SslConfig().environmentLoader(this.environmentLoader).build();
        }
        return this;
    }

}

