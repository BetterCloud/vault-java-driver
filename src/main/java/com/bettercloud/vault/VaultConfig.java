package com.bettercloud.vault;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

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
public class VaultConfig {
    public static final String ENV_VAR_VAULT_ADDR = "VAULT_ADDR";
    public static final String ENV_VAR_VAULT_TOKEN = "VAULT_TOKEN";
    public static final String ENV_VAR_VAULT_SSL_CERT = "VAULT_SSL_CERT";
    public static final String ENV_VAR_VAULT_SSL_VERIFY = "VAULT_SSL_VERIFY";
    public static final String ENV_VAR_VAULT_OPEN_TIMEOUT = "VAULT_OPEN_TIMEOUT";
    public static final String ENV_VAR_VAULT_READ_TIMEOUT = "VAULT_READ_TIMEOUT";

    public static final String ENV_VAR_VAULT_CLIENT_KEYSTORE = "VAULT_CLIENT_KEYSTORE";
    public static final String ENV_VAR_VAULT_CLIENT_KEYSTORE_PASSWORD = "VAULT_CLIENT_KEYSTORE_PASSWORD";
    public static final String ENV_VAR_VAULT_CLIENT_TRUSTSTORE = "VAULT_CLIENT_TRUSTSTORE";
    public static final String ENV_VAR_VAULT_CLIENT_TRUSTSTORE_PASSWORD = "VAULT_CLIENT_TRUSTSTORE_PASSWORD";

    /**
     * <p>The code used to load environment variables is encapsulated within an inner class,
     * so that a mock version of that environment loader can be used by unit tests.</p>
     */
    static class EnvironmentLoader {
        public String loadVariable(final String name) {
            String value = null;
            if ("VAULT_TOKEN".equals(name)) {

                // Special handling for the VAULT_TOKEN variable, since it can be read from the filesystem if it's not
                // found in the environment
                if (System.getenv("VAULT_TOKEN") != null) {
                    // Found it in the environment
                    value = System.getenv(name);
                } else {
                    // Not in the environment, looking for a ".vault-token" file in the executing user's home directory instead
                    try {
                        final byte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.home")).resolve(".vault-token"));
                        value = new String(bytes, "UTF-8").trim();
                    } catch (IOException e) {
                        // No-op... there simple isn't a token value available
                    }
                }
            } else {

                // Normal handling for all other variables.  We just check the environment.
                value = System.getenv(name);
            }
            return value;
        }
    }

    private EnvironmentLoader environmentLoader;
    private String address;
    private String token;
    private String sslPemUTF8;
    private Boolean sslVerify;
    private Integer openTimeout;
    private Integer readTimeout;
    private int maxRetries;
    private int retryIntervalMilliseconds;

    private File keystore;
    private String keystorePassword;
    private File truststore;
    private String truststorePassword;

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
     * @throws VaultException If any error occurs while loading and parsing config values
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
     * @throws VaultException If any error occurs while loading and parsing config values
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
     * @throws VaultException If any error occurs while loading and parsing config values
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
     * @throws VaultException If any error occurs while loading and parsing config values
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
     * @param environmentLoader An environment variable loader implementation (presumably a mock)
     * @return This object, with environmentLoader populated, ready for additional builder-pattern method calls or else finalization with the {@link this#build()} method
     */
    VaultConfig environmentLoader(final EnvironmentLoader environmentLoader) {
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
     * @return This object, with sslPemUTF8 populated, ready for additional builder-pattern method calls or else finalization with the build() method
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
     * @return This object, with sslPemFile populated, ready for additional builder-pattern method calls or else finalization with the build() method
     * @throws VaultException If any error occurs while loading and parsing the PEM file
     */
    public VaultConfig sslPemFile(final File sslPemFile) throws VaultException {
        try (final InputStream input = new FileInputStream(sslPemFile)){
            this.sslPemUTF8 = inputStreamToUTF8(input);
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
     * @return This object, with sslPemResource populated, ready for additional builder-pattern method calls or else finalization with the build() method
     * @throws VaultException If any error occurs while loading and parsing the PEM file
     */
    public VaultConfig sslPemResource(final String classpathResource) throws VaultException {
        try (final InputStream input = this.getClass().getResourceAsStream(classpathResource)){
            this.sslPemUTF8 = inputStreamToUTF8(input);
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
     * @return This object, with sslVerify populated, ready for additional builder-pattern method calls or else finalization with the build() method
     */
    public VaultConfig sslVerify(final Boolean sslVerify) {
        this.sslVerify = sslVerify;
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
     * Configure a JKS Keystore for HTTPS mutual authentication
     * 
     * @param jksKeystore - Absolute file path of the JKS keystore
     * @return This object, with keystore populated, ready for additional builder-pattern method calls or else
     *         finalization with the build() method
     */
    public VaultConfig keystoreFile(final String jksKeystore) {
        this.keystore = new File(jksKeystore);
        return this;
    }

    /**
     * Configure a JKS Keystore for HTTPS mutual authentication
     * 
     * @param jksKeystore - File reference to the JKS keystore
     * @return This object, with keystore populated, ready for additional builder-pattern method calls or else
     *         finalization with the build() method
     */
    public VaultConfig keystoreFile(final File jksKeystore) {
        this.keystore = jksKeystore;
        return this;
    }

    /**
     * Password to open the JKS keystore
     * 
     * @param keystorePassword - Password (in plain text)
     * @return This object, with keystore password populated, ready for additional builder-pattern method calls or else
     *         finalization with the build() method
     */
    public VaultConfig keystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    /**
     * <p>
     * Configure an alternate truststore to be used for all HTTPS connections with Vault
     * </p>
     * <p>
     * You may set your own truststore if you do not wish to use Java's default truststore
     * </p>
     * 
     * @param truststore - File reference to the JKS keystore containing all your trusted CA certs
     * @return This object, with truststore populated, ready for additional builder-pattern method calls or else
     *         finalization with the build() method
     */
    public VaultConfig truststore(File truststore) {
        this.truststore = truststore;
        return this;
    }

    /**
     * Password to open the truststore
     * 
     * @param truststorePassword - Password (in plain text)
     * @return This object, with truststore password populated, ready for additional builder-pattern method calls or
     *         else finalization with the build() method
     */
    public VaultConfig truststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
        return this;
    }

    /**
     * <p>
     * Sets the maximum number of times that an API operation will retry upon failure.
     * </p>
     *
     * <p>
     * This method is not meant to be called from application-level code outside of this package (hence the
     * <code>protected</code> access level. It is meant to be invoked via <code>Vault.withRetries()</code> in a builder
     * pattern DSL-style.
     * </p>
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
            final String addressFromEnv = environmentLoader.loadVariable(ENV_VAR_VAULT_ADDR);
            if (addressFromEnv != null) {
                this.address = addressFromEnv;
            } else {
                throw new VaultException("No address is set");
            }
        }

        String token = environmentLoader.loadVariable(ENV_VAR_VAULT_TOKEN);
        if (this.token == null && token != null) {
            this.token = token;
        }

        String sslCert = environmentLoader.loadVariable(ENV_VAR_VAULT_SSL_CERT);
        if (this.sslPemUTF8 == null && sslCert != null && !sslCert.trim().isEmpty()) {
            final File pemFile = new File(sslCert.trim());
            try (final InputStream input = new FileInputStream(pemFile)) {
                this.sslPemUTF8 = inputStreamToUTF8(input);
            } catch (IOException e) {
                throw new VaultException(e);
            }
        }

        String sslVerify = environmentLoader.loadVariable(ENV_VAR_VAULT_SSL_VERIFY);
        if (this.sslVerify == null && sslVerify != null) {
            this.sslVerify = Boolean.valueOf(sslVerify);
        }

        String openTimeout = environmentLoader.loadVariable(ENV_VAR_VAULT_OPEN_TIMEOUT);
        if (this.openTimeout == null && openTimeout != null) {
            try {
                this.openTimeout = Integer.valueOf(openTimeout);
            } catch (NumberFormatException e) {
                System.err.printf(
                    "The \"%s\" environment variable contains value \"%s\", which cannot be parsed as an integer timeout period.%n",
                    ENV_VAR_VAULT_OPEN_TIMEOUT, openTimeout);
            }
        }

        String readTimeout = environmentLoader.loadVariable(ENV_VAR_VAULT_READ_TIMEOUT);
        if (this.readTimeout == null && readTimeout != null) {
            try {
                this.readTimeout = Integer.valueOf(readTimeout);
            } catch (NumberFormatException e) {
                System.err.printf(
                    "The \"%s\" environment variable contains value \"%s\", which cannot be parsed as an integer timeout period.%n",
                    ENV_VAR_VAULT_READ_TIMEOUT, readTimeout);
            }
        }

        String keystoreFile = environmentLoader.loadVariable(ENV_VAR_VAULT_CLIENT_KEYSTORE);
        if (this.keystore == null && keystoreFile != null && !keystoreFile.trim().isEmpty()) {
            this.keystore = new File(keystoreFile.trim());
        }

        String keystorePassword = environmentLoader.loadVariable(ENV_VAR_VAULT_CLIENT_KEYSTORE_PASSWORD);
        if (this.keystorePassword == null && keystorePassword != null) {
            this.keystorePassword = keystorePassword;
        }


        String trustStoreFile = environmentLoader.loadVariable(ENV_VAR_VAULT_CLIENT_TRUSTSTORE);
        if (this.truststore == null && trustStoreFile != null && !trustStoreFile.trim().isEmpty()) {
            this.truststore = new File(trustStoreFile.trim());
        }

        String truststorePassword = environmentLoader.loadVariable(ENV_VAR_VAULT_CLIENT_TRUSTSTORE_PASSWORD);
        if (this.truststorePassword == null && truststorePassword != null) {
            this.truststorePassword = truststorePassword;
        }

        return this;
    }

    public String getAddress() {
        return address;
    }

    public String getToken() {
        return token;
    }

    public String getSslPemUTF8() {
        return sslPemUTF8;
    }

    public Boolean isSslVerify() {
        return sslVerify;
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

    public File getKeystore() {
        return this.keystore;
    }

    public String getKeystorePassword() {
        return this.keystorePassword;
    }

    public File getTruststore() {
        return truststore;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    private String inputStreamToUTF8(final InputStream input) throws IOException {
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

