package com.bettercloud.vault;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

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
 *                              .sslConfig(new SslConfig().verify(true).build())
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
     * <p>Default constructor.  Should be used in conjunction with the builder pattern, calling additional
     * property setter methods and ultimately finishing with a call to <code>build()</code>.</p>
     *
     * <p>Note that when using this builder pattern approach, you must either set <code>address</code>
     * and <code>token</code> explicitly, or else have them available as runtime environment variables.</p>
     */
    public VaultConfig() throws VaultException {
    }

    /**
     * <p>The code used to load environment variables is encapsulated here, so that a mock version of that environment
     * loader can be used by unit tests.</p>
     *
     * <p>This method is used by unit tests, to inject a mock environment variable when constructing a
     * <code>VaultConfig</code> instance using the builder pattern approach rather than the convenience constructor.
     * There really shouldn't ever be a need to call this method outside of a unit test context (hence the
     * <code>protected</code> access level).</p>
     *
     * @param environmentLoader An environment variable loader implementation (presumably a mock)
     * @return This object, with environmentLoader populated, ready for additional builder-pattern method calls or else finalization with the {@link this#build()} method
     */
    protected VaultConfig environmentLoader(final EnvironmentLoader environmentLoader) {
        this.environmentLoader = environmentLoader;
        return this;
    }

    /**
     * <p>Sets the address (URL) of the Vault server instance to which API calls should be sent.
     * E.g. <code>http://127.0.0.1:8200</code>.</p>
     *
     * <p>If no address is explicitly set, either by this method in a builder pattern approach or else by one of the
     * convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_ADDR</code> environment
     * variable.</p>
     *
     * <p><code>address</code> is required for the Vault driver to function.  If you do not supply it explicitly AND no
     * environment variable value is found, then initialization of the <code>VaultConfig</code> object will fail.</p>
     *
     * @param address The Vault server base URL
     * @return This object, with address populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig address(final String address) {
        this.address = address;
        return this;
    }

    /**
     * <p>Sets the token used to access Vault.</p>
     *
     * <p>If no token is explicitly set, either by this method in a builder pattern approach or else by one of the
     * convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_TOKEN</code> environment
     * variable.</p>
     *
     * <p>There are some cases where you might want to instantiate a <code>VaultConfig</code> object without a token
     * (e.g. you plan to retrieve a token programmatically, with a call to the "userpass" auth backend, and populate
     * it prior to making any other API calls).  In such use cases, you can still use either the builder pattern
     * approach or the single-argument convenience constructor.</p>
     *
     * @param token The token to use for accessing Vault
     * @return This object, with token populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig token(final String token) {
        this.token = token;
        return this;
    }

    /**
     * TODO: Document
     *
     * @param sslConfig
     * @return
     */
    public VaultConfig sslConfig(final SslConfig sslConfig) {
        this.sslConfig = sslConfig;
        return this;
    }

    /**
     * <p>The number of seconds to wait before giving up on establishing an HTTP(S) connection to the Vault server.</p>
     *
     * <p>If no openTimeout is explicitly set, either by this method in a builder pattern approach or else by one of
     * the convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_OPEN_TIMEOUT</code>
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
     * <p>If no readTimeout is explicitly set, either by this method in a builder pattern approach or else by one of
     * the convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_READ_TIMEOUT</code>
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

    /**
     * TODO: Document
     *
     * @param input
     * @return
     * @throws IOException
     */
    protected static String inputStreamToUTF8(final InputStream input) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        final StringBuilder utf8 = new StringBuilder("");
        String str;
        while ((str = in.readLine()) != null) {
            // String concatenation is less efficient, but for some reason the line-breaks (which are necessary
            // for Java to correctly parse SSL certs) are stripped off when using a StringBuilder.
            utf8.append(str).append(System.lineSeparator());
        }
        in.close();
        return utf8.toString();
    }

}

