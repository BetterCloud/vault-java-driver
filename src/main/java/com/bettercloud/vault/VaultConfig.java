package com.bettercloud.vault;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
    private String sslPemUTF8;
    private Boolean sslVerify;
    private Integer timeout;
    private Integer sslTimeout;
    private Integer openTimeout;
    private Integer readTimeout;
    private int maxRetries;
    private int retryIntervalMilliseconds;

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
     * <p>Although <code>address</code> and <code>token</code> are the only two properties explicitly passed, the
     * constructor will still look to the runtime environment variables to populate any other fields when values
     * are present.</p>
     *
     * <p>When using this approach to creating a <code>VaultConfig</code> instance, you should NOT make additional
     * setter method calls after construction.  If you need other properties set explicitly, then use the builder
     * pattern approach.</p>
     *
     * @param address The URL of the target Vault server
     * @param token The access token to enable Vault access
     * @throws VaultException
     */
    public VaultConfig(final String address, final String token) throws VaultException {
        this(address, token, new EnvironmentLoader());
    }

    /**
     * <p>A convenience constructor, for quickly creating a <code>VaultConfig</code> instance with its
     * <code>address</code> field populated.</p>
     *
     * <p>While the other convenience constructor requires root token parameter, this constructor version does not.
     * So it IS possible to construct a <code>VaultConfig</code> object with no root token present.  However, such
     * an object will be of no use with most actual Vault API calls.  This constructor is therefore meant to be used
     * when you plan to programmatically retrieve a token (e.g. from the "userpass" backend) and populate it prior
     * to making other API calls.</p>
     *
     * <p>When using this approach to creating a <code>VaultConfig</code> instance, you should NOT make additional
     * setter method calls after construction... other than the token scenario described immediately above.  If you
     * need any other properties set explicitly, then use the builder pattern approach.</p>
     *
     * @param address The URL of the target Vault server
     * @throws VaultException
     */
    public VaultConfig(final String address) throws VaultException {
        this(address, new EnvironmentLoader());
    }

    /**
     * An overloaded version of the normal convenience constructor, used by unit tests to inject a mock environment
     * variable loader and validate that loading logic.
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

    /**
     * An overloaded version of the normal convenience constructor, used by unit tests to inject a mock environment
     * variable loader and validate that loading logic.
     *
     * @param address The URL of the target Vault server
     * @param environmentLoader A (mock) environment loader implementation
     * @throws VaultException
     */
    protected VaultConfig(final String address, final EnvironmentLoader environmentLoader) throws VaultException {
        this.address = address;
        this.environmentLoader = environmentLoader;
        build();
    }

    /**
     * <p>The code used to load environment variables is encapsulated within an inner class, so that a mock version of
     * that environment loader can be used by unit tests.</p>
     *
     * <p>This method is used by unit tests, to inject a mock environment variable when constructing a
     * <code>VaultConfig</code> instance using the builder pattern approach rather than the convenience constructor.
     * There really shouldn't ever be a need to call this method outside of a unit test context (hence the
     * <code>protected</code> access level).</p>
     *
     * @param environmentLoader An environment variable loader implementation (presumably a mock).
     * @return
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
     * @return
     */
    public VaultConfig address(final String address) {
        this.address = address;
        return this;
    }

    /**
     * <p>Sets the root token used to access Vault.</p>
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
     * @param token
     * @return
     */
    public VaultConfig token(final String token) {
        this.token = token;
        return this;
    }

    /**
     * TODO: Not yet being used.  Implement...
     *
     * <p>If no proxyAddress is explicitly set, either by this method in a builder pattern approach or else by one of
     * the convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_PROXY_ADDRESS</code>
     * environment variable.</p>
     *
     * @param proxyAddress
     * @return
     */
    public VaultConfig proxyAddress(final String proxyAddress) {
        this.proxyAddress = proxyAddress;
        return this;
    }

    /**
     * TODO: Not yet being used.  Implement...
     *
     * <p>If no proxyPort is explicitly set, either by this method in a builder pattern approach or else by one of the
     * convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_PROXY_PORT</code>
     * environment variable.</p>
     *
     * @param proxyPort
     * @return
     */
    public VaultConfig proxyPort(final Integer proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    /**
     * TODO: Not yet being used.  Implement...
     *
     * <p>If no proxyUsername is explicitly set, either by this method in a builder pattern approach or else by one of
     * the convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_PROXY_USERNAME</code>
     * environment variable.</p>
     *
     * @param proxyUsername
     * @return
     */
    public VaultConfig proxyUsername(final String proxyUsername) {
        this.proxyUsername = proxyUsername;
        return this;
    }

    /**
     * TODO: Not yet being used.  Implement...
     *
     * <p>If no proxyPassword is explicitly set, either by this method in a builder pattern approach or else by one of
     * the convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_PROXY_PASSWORD</code>
     * environment variable.</p>
     *
     * @param proxyPassword
     * @return
     */
    public VaultConfig proxyPassword(final String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    /**
     * <p>An X.509 certificate, to use when communicating with Vault over HTTPS.  This method accepts a string
     * containing the certificate data.  This string should meet the following requirements:</p>
     *
     * <ul>
     *     <li>Contain an unencrypted X.509 certificate, in PEM format.</li>
     *     <li>Use UTF-8 encoding.</li>
     *     <li>
     *          Contain a line-break between the certificate header (e.g. "-----BEGIN CERTIFICATE-----") and the
     *          rest of the certificate content.  It doesn't matter whether or not there are additional line
     *          breaks within the certificate content, or whether there is a line break before the certificate
     *          footer (e.g. "-----END CERTIFICATE-----").  But the Java standard library will fail to properly
     *          process the certificate without a break following the header
     *          (see http://www.doublecloud.org/2014/03/reading-x-509-certificate-in-java-how-to-handle-format-issue/).
     *      </li>
     * </ul>
     *
     * <p>If no certificate data is provided, either by this method or <code>sslPemFile()</code>
     * or <code>sslPemResource()</code>, then <code>VaultConfig</code> will look to the
     * <code>VAULT_SSL_CERT</code> environment variable.</p>
     *
     * @param sslPemUTF8 An X.509 certificate, in unencrypted PEM format with UTF-8 encoding.
     * @return
     */
    public VaultConfig sslPemUTF8(final String sslPemUTF8) {
        this.sslPemUTF8 = sslPemUTF8;
        return this;
    }

    /**
     * <p>An X.509 certificate, to use when communicating with Vault over HTTPS.  This method accepts the path of
     * a file containing the certificate data.  This file's contents should meet the following requirements:</p>
     *
     * <ul>
     *     <li>Contain an unencrypted X.509 certificate, in PEM format.</li>
     *     <li>Use UTF-8 encoding.</li>
     *     <li>
     *          Contain a line-break between the certificate header (e.g. "-----BEGIN CERTIFICATE-----") and the
     *          rest of the certificate content.  It doesn't matter whether or not there are additional line
     *          breaks within the certificate content, or whether there is a line break before the certificate
     *          footer (e.g. "-----END CERTIFICATE-----").  But the Java standard library will fail to properly
     *          process the certificate without a break following the header
     *          (see http://www.doublecloud.org/2014/03/reading-x-509-certificate-in-java-how-to-handle-format-issue/).
     *      </li>
     * </ul>
     *
     * <p>If no certificate data is provided, either by this method or <code>sslPemResource()</code>
     * or <code>sslPemUTF8()</code>, then <code>VaultConfig</code> will look to the
     * <code>VAULT_SSL_CERT</code> environment variable.</p>
     *
     * @param sslPemFile The path of a file containing an X.509 certificate, in unencrypted PEM format with UTF-8 encoding.
     * @return
     */
    public VaultConfig sslPemFile(final File sslPemFile) throws VaultException {
        try {
            final InputStream input = new FileInputStream(sslPemFile);
            this.sslPemUTF8 = inputStreamToUTF8(input);
            input.close();
            this.sslPemFile = sslPemFile.getPath();
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return this;
    }

    /**
     * <p>An X.509 certificate, to use when communicating with Vault over HTTPS.  This method accepts the path of
     * a classpath resource containing the certificate data (e.g. you've bundled the cert into your library or
     * application's JAR/WAR/EAR file).  This resource's contents should meet the following requirements:</p>
     *
     * <ul>
     *     <li>Contain an unencrypted X.509 certificate, in PEM format.</li>
     *     <li>Use UTF-8 encoding.</li>
     *     <li>
     *          Contain a line-break between the certificate header (e.g. "-----BEGIN CERTIFICATE-----") and the
     *          rest of the certificate content.  It doesn't matter whether or not there are additional line
     *          breaks within the certificate content, or whether there is a line break before the certificate
     *          footer (e.g. "-----END CERTIFICATE-----").  But the Java standard library will fail to properly
     *          process the certificate without a break following the header
     *          (see http://www.doublecloud.org/2014/03/reading-x-509-certificate-in-java-how-to-handle-format-issue/).
     *      </li>
     * </ul>
     *
     * <p>If no certificate data is provided, either by this method or <code>sslPemFile()</code>
     * or <code>sslPemUTF8()</code>, then <code>VaultConfig</code> will look to the
     * <code>VAULT_SSL_CERT</code> environment variable.</p>
     *
     * @param classpathResource The path of a classpath resource containing an X.509 certificate, in unencrypted PEM format with UTF-8 encoding.
     * @return
     * @throws VaultException
     */
    public VaultConfig sslPemResource(final String classpathResource) throws VaultException {
        try {
            final InputStream input = this.getClass().getResourceAsStream(classpathResource);
            this.sslPemUTF8 = inputStreamToUTF8(input);
            input.close();
            this.sslPemFile = classpathResource;
        } catch (IOException e) {
            throw new VaultException(e);
        }
        return this;
    }

    /**
     * <p>Whether or not HTTPS connections to the Vault server should verify that a valid SSL certificate is being
     * used.  Unless this is set to <code>false</code>, the default behavior is to always verify SSL certificates.</p>
     *
     * <p>SSL CERTIFICATE VERIFICATION SHOULD NOT BE DISABLED IN PRODUCTION!  This feature is made available to
     * facilitate development or testing environments, where you might be using a self-signed cert that will not
     * pass verification.  However, even if you are using a self-signed cert on your Vault server, you can still leave
     * SSL verification enabled and have your application supply the cert using <code>sslPemFile()</code>,
     * <code>sslPemResource()</code>, or <code>sslPemUTF8()</code>.</p>
     *
     * <p>If no sslVerify is explicitly set, either by this method in a builder pattern approach or else by one of the
     * convenience constructors, then <code>VaultConfig</code> will look to the <code>VAULT_SSL_VERIFY</code>
     * environment variable.</p>
     *
     * @param sslVerify Whether or not to verify the SSL certificate used by Vault with HTTPS connections.  Default is <code>true</code>.
     * @return
     */
    public VaultConfig sslVerify(final Boolean sslVerify) {
        this.sslVerify = sslVerify;
        return this;
    }

    /**
     * This field is unused, and will be removed in the next version of the driver.  Use <code>openTimeout</code>
     * and <code>readTimeout</code>.
     *
     * @param timeout
     * @return
     */
    @Deprecated
    public VaultConfig timeout(final Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * This field is unused, and will be removed in the next version of the driver.  Use <code>openTimeout</code>
     * and <code>readTimeout</code>.
     *
     * @param sslTimeout
     * @return
     */
    @Deprecated
    public VaultConfig sslTimeout(final Integer sslTimeout) {
        this.sslTimeout = sslTimeout;
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
     * @return
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
     * @return
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
     * @return
     * @throws VaultException If the <code>address</code> field was left unset, and there is no <code>VAULT_ADDR</code> environment variable value with which to populate it.
     */
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
        if (this.token == null && environmentLoader.loadVariable("VAULT_TOKEN") != null) {
            this.token = environmentLoader.loadVariable("VAULT_TOKEN");
        }
        if (this.proxyAddress == null && environmentLoader.loadVariable("VAULT_PROXY_ADDRESS") != null) {
            this.proxyAddress = environmentLoader.loadVariable("VAULT_PROXY_ADDRESS");
        }
        if (this.proxyPort == null && environmentLoader.loadVariable("VAULT_PROXY_PORT") != null) {
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
        if (this.sslPemUTF8 == null && environmentLoader.loadVariable("VAULT_SSL_CERT") != null) {
            final File pemFile = new File(environmentLoader.loadVariable("VAULT_SSL_CERT"));
            try {
                final InputStream input = new FileInputStream(pemFile);
                this.sslPemUTF8 = inputStreamToUTF8(input);
                input.close();
            } catch (IOException e) {
                throw new VaultException(e);
            }
        }
        if (this.sslVerify == null && environmentLoader.loadVariable("VAULT_SSL_VERIFY") != null) {
            this.sslVerify = Boolean.valueOf(environmentLoader.loadVariable("VAULT_SSL_VERIFY"));
        }
        if (this.openTimeout == null && environmentLoader.loadVariable("VAULT_OPEN_TIMEOUT") != null) {
            try {
                this.openTimeout = Integer.valueOf(environmentLoader.loadVariable("VAULT_OPEN_TIMEOUT"));
            } catch (NumberFormatException e) {
                System.err.printf("The \"VAULT_OPEN_TIMEOUT\" environment variable contains value \"%s\", which cannot be parsed as an integer timeout period.\n",
                        environmentLoader.loadVariable("VAULT_OPEN_TIMEOUT"));
            }
        }
        if (this.readTimeout == null && environmentLoader.loadVariable("VAULT_READ_TIMEOUT") != null) {
            try {
                this.readTimeout = Integer.valueOf(environmentLoader.loadVariable("VAULT_READ_TIMEOUT"));
            } catch (NumberFormatException e) {
                System.err.printf("The \"VAULT_READ_TIMEOUT\" environment variable contains value \"%s\", which cannot be parsed as an integer timeout period.\n",
                        environmentLoader.loadVariable("VAULT_READ_TIMEOUT"));
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

    @Deprecated
    public String getSslPemFile() {
        return sslPemFile;
    }

    public String getSslPemUTF8() {
        return sslPemUTF8;
    }

    public Boolean isSslVerify() {
        return sslVerify;
    }

    @Deprecated
    public Integer getTimeout() {
        return timeout;
    }

    @Deprecated
    public Integer getSslTimeout() {
        return sslTimeout;
    }

    public Integer getOpenTimeout() {
        return openTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRetryIntervalMilliseconds() {
        return retryIntervalMilliseconds;
    }

    private String inputStreamToUTF8(final InputStream input) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String utf8 = "";
        String str;
        while ((str = in.readLine()) != null) {
            // String concatenation is less efficient, but for some reason the line-breaks (which are necessary
            // for Java to correctly parse SSL certs) are stripped off when using a StringBuilder.
            utf8 += str + System.lineSeparator();//NOPMD
        }
        in.close();
        return utf8;
    }

}

