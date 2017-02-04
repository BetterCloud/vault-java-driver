package com.bettercloud.vault.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * <p>A simple client for issuing HTTP requests.  Supports the HTTP verbs:</p>
 * <ul>
 *     <li>GET</li>
 *     <li>POST</li>
 *     <li>PUT</li>
 *     <li>DELETE</li>
 * </ul>
 *
 * <p><code>Rest</code> uses the Builder pattern to provide a basic DSL for usage.  Methods for configuring an HTTP
 * request (i.e. <code>url()</code>, <code>parameter()</code>, and <code>header()</code>) are designed to be chained
 * together, while methods corresponding to the HTTP verbs are terminating operations:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final RestResponse getResponse = new Rest()
 *                              .url("https://httpbin.org/get")
 *                              .header("header-1", "foo")
 *                              .header("header-2", "bar")
 *                              .parameter("param-1", "up")
 *                              .parameter("param-2", "down")
 *                              .get();
 *
 * final RestResponse postResponse = new Rest()
 *                              .url("https://httpbin.org/post")
 *                              .header("header-1", "foo")
 *                              .header("header-2", "bar")
 *                              .body( jsonString.getBytes("UTF-8") )
 *                              .post();
 * }</pre>
 * </blockquote>
 *
 * <p>Header and parameter names and values are url-encoded by the Rest client prior to sending the request.  The URL
 * string should be url-encoded by you (if necessary) prior to passing it.</p>
 */
public class Rest {

    private String urlString;
    private byte[] body;
    private final Map<String, String> parameters = new TreeMap<String, String>();
    private final Map<String, String> headers = new TreeMap<String, String>();

    private Integer connectTimeoutSeconds;
    private Integer readTimeoutSeconds;
    private Boolean sslVerification;

    @SuppressWarnings("unused")
    @Deprecated
    private String sslPemUTF8;

    private File keystoreFile;
    private File truststore;
    private String keystorePassword;
    private String truststorePassword;

    private static final Object MUTEX = new Object();
    private SSLContext vaultSSLContext = null;

    /**
     * <p>Sets the base URL to which the HTTP request will be sent.  The URL may or may not include query parameters
     * (e.g. <code>http://httpbin.org/get?param-1=foo</code>).</p>
     *
     * <p>Depending on which HTTP verb is ultimately used, than any additional parameters set via the
     * <code>parameters()</code> method may be appending to this URL.</p>
     *
     * <p>Either way, the responsibility for any url-encoding of this base URL string belongs to the caller.</p>
     *
     * @param urlString A URL string, with any necessary url-encoding already applied @return The <code>Rest</code> instance itself
     * @return This object, with urlString populated, ready for other builder-pattern config methods or an HTTP verb method
     */
    public Rest url(final String urlString) {
        this.urlString = urlString;
        return this;
    }

    /**
     * <p>Sets a binary payload that will be sent as the request body for POST or PUT requests.  Any value set here
     * will be ignored for GET requests.  Conversely, if a value IS set here... then any additional parameter values
     * set by <code>parameter()</code> will be ignored for POST or PUT requests.</p>
     *
     * @param body The payload to send with a POST or PUT request (e.g. a JSON string)
     * @return This object, with body populated, ready for other builder-pattern config methods or an HTTP verb method
     */
    public Rest body(final byte[] body) {
        this.body = body == null ? null : Arrays.copyOf(body, body.length);
        return this;
    }

    /**
     * <p>Adds a parameter to be sent with the HTTP request.  Depending on which HTTP verb is ultimately used, this
     * parameter may either be appended to the URL or else posted with the request body.  Either way, both the
     * parameter name and value will be automatically url-encoded by the Rest client.</p>
     *
     * <p>For POST and PUT requests, these parameters will only be sent in the request body if that body is otherwise
     * unset.  In other words, if the <code>body()</code> method is invoked, then <code>parameter()</code> invocations
     * will be ignored for a POST or PUT.</p>
     *
     * <p>This method may be chained together repeatedly, to pass multiple parameters with a request.  When the
     * request is ultimately sent, the parameters will be sorted by their names.</p>
     *
     * @param name The raw parameter name (not url-encoded)
     * @param value The raw parameter value (not url-encoded)
     * @return This object, with a parameter added, ready for other builder-pattern config methods or an HTTP verb method
     * @throws RestException If any error occurs, or unexpected response received from Vault
     */
    public Rest parameter(final String name, final String value) throws RestException {
        try {
            this.parameters.put(URLEncoder.encode(name, "UTF-8"), URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RestException(e);
        }
        return this;
    }

    /**
     * <p>Adds a header to be send with the HTTP request.</p>
     *
     * <p>Both the header name and value will be automatically url-encoded by the Rest client.</p>
     *
     * <p>This method may be chained together repeatedly, to pass multiple headers with a request.  When the request
     * is ultimately sent, the headers will be sorted by their names.</p>
     *
     * @param name The raw header name (not url-encoded)
     * @param value The raw header value (not url-encoded)
     * @return This object, with a header added, ready for other builder-pattern config methods or an HTTP verb method
     * @throws RestException If any error occurs, or unexpected response received from Vault
     */
    public Rest header(final String name, final String value) throws RestException {
        try {
            this.headers.put(URLEncoder.encode(name, "UTF-8"), URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RestException(e);
        }
        return this;
    }

    /**
     * <p>The number of seconds to wait before giving up on establishing an HTTP(S) connection.</p>
     *
     * @param connectTimeoutSeconds Number of seconds to wait for an HTTP(S) connection to successfully establish
     * @return This object, with connectTimeoutSeconds populated, ready for other builder-pattern config methods or an HTTP verb method
     */
    public Rest connectTimeoutSeconds(final Integer connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        return this;
    }

    /**
     * <p>After an HTTP(S) connection has already been established, this is the number of seconds to wait for all
     * data to finish downloading.</p>
     *
     * @param readTimeoutSeconds Number of seconds to wait for all data to be retrieved from an established HTTP(S) connection
     * @return This object, with readTimeoutSeconds populated, ready for other builder-pattern config methods or an HTTP verb method
     */
    public Rest readTimeoutSeconds(final Integer readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
        return this;
    }

    /**
     * <p>Whether or not HTTPS connections should verify that the server has a valid SSL certificate.
     * Unless this is set to <code>false</code>, the default behavior is to always verify SSL certificates.</p>
     *
     * <p>SSL CERTIFICATE VERIFICATION SHOULD NOT BE DISABLED IN PRODUCTION!  This feature is made available to
     * facilitate development or testing environments, where you might be using a self-signed cert that will not
     * pass verification.  However, even if you are using a self-signed cert on your server, you can still leave
     * SSL verification enabled and have your application supply the cert using <code>sslPemFile()</code>,
     * <code>sslPemResource()</code>, or <code>sslPemUTF8()</code>.</p>
     *
     * @param sslVerification Whether or not to verify the SSL certificate used by the server with HTTPS connections.  Default is <code>true</code>.
     * @return This object, with sslVerification populated, ready for other builder-pattern config methods or an HTTP verb method
     */
    public Rest sslVerification(final Boolean sslVerification) {
        this.sslVerification = sslVerification;
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
     * @param pemFileContents An X.509 certificate, in unencrypted PEM format with UTF-8 encoding.
     * @return This object, with sslPemUTF8 populated, ready for other builder-pattern config methods or an HTTP verb method
     */
    public Rest sslPemUTF8(final String pemFileContents) {
        this.sslPemUTF8 = pemFileContents;
        return this;
    }

    public Rest keystore(File keystore) {
        this.keystoreFile = keystore;
        return this;
    }

    public Rest keystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    public Rest truststore(File truststore) {
        this.truststore = truststore;
        return this;
    }

    public Rest truststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
        return this;
    }

    /**
     * <p>Executes an HTTP GET request with the settings already configured.  Parameters and headers are optional, but
     * a <code>RestException</code> will be thrown if the caller has not first set a base URL with the
     * <code>url()</code> method.</p>
     *
     * <p>If a body payload has been set through the <code>body()</code> method, then it will be ignored when sending
     * a GET request.</p>
     *
     * @return The result of the HTTP operation
     * @throws RestException If an error occurs, or an unexpected response received
     */
    public RestResponse get() throws RestException {
        if (urlString == null) {
            throw new RestException("No URL is set");
        }
        try {
            if (!parameters.isEmpty()) {
                // Append parameters to existing query string, or create one
                if (urlString.indexOf('?') == -1) {
                    urlString = urlString + "?" + parametersToQueryString();
                } else {
                    urlString = urlString + "&" + parametersToQueryString();
                }
            }
            // Initialize HTTP(S) connection, and set any header values
            final URLConnection connection = initURLConnection(urlString, "GET");
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Get the resulting status code
            final int statusCode = connectionStatus(connection);
            // Download and parse response
            final String mimeType = connection.getContentType();
            final byte[] body = responseBodyBytes(connection);
            return new RestResponse(statusCode, mimeType, body);
        } catch (Exception e) {
            throw new RestException(e);
        }
    }

    /**
     * Executes an HTTP POST request with the settings already configured.  Parameters and headers are optional, but a
     * <code>RestException</code> will be thrown if the caller has not first set a base URL with the
     * <code>url()</code> method.
     *
     * <p>CGI parameters can always be passed via a query string on the URL.  Also, parameter values set via the
     * <code>parameter()</code> method will be sent with the POST request as form data.  However, if a body payload
     * is provided via the <code>body()</code> method, then that takes precedence over any parameters set via
     * <code>parameter()</code>, and those values will be discarded.</p>
     *
     * @return The result of the HTTP operation
     * @throws RestException If an error occurs, or an unexpected response received
     */
    public RestResponse post() throws RestException {
        return postOrPutImpl(true);
    }

    /**
     * Executes an HTTP PUT request with the settings already configured.  Parameters and headers are optional, but a
     * <code>RestException</code> will be thrown if the caller has not first set a base URL with the
     * <code>url()</code> method.
     *
     * <p>CGI parameters can always be passed via a query string on the URL.  Also, parameter values set via the
     * <code>parameter()</code> method will be sent with the PUT request as form data.  However, if a body payload
     * is provided via the <code>body()</code> method, then that takes precedence over any parameters set via
     * <code>parameter()</code>, and those values will be discarded.</p>
     *
     * @return The result of the HTTP operation
     * @throws RestException If an error occurs, or an unexpected response received
     */
    public RestResponse put() throws RestException {
        return postOrPutImpl(false);
    }

    /**
     * Executes an HTTP DELETE request with the settings already configured.  Parameters and headers are optional,
     * but a <code>RestException</code> will be thrown if the caller has not first set a base URL with the
     * <code>url()</code> method.
     *
     * Note that any parameters are set in the query string.  This method does not send a request body, as some
     * HTTP servers will ignore it for DELETE requests.
     *
     * @return The result of the HTTP operation
     * @throws RestException If an error occurs, or an unexpected response received
     */
    public RestResponse delete() throws RestException {
        if (urlString == null) {
            throw new RestException("No URL is set");
        }
        try {
            if (!parameters.isEmpty()) {
                // Append parameters to existing query string, or create one
                if (urlString.indexOf('?') == -1) {
                    urlString = urlString + "?" + parametersToQueryString();
                } else {
                    urlString = urlString + "&" + parametersToQueryString();
                }
            }
            // Initialize HTTP(S) connection, and set any header values
            final URLConnection connection = initURLConnection(urlString, "DELETE");
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Get the resulting status code
            final int statusCode = connectionStatus(connection);
            // Download and parse response
            final String mimeType = connection.getContentType();
            final byte[] body = responseBodyBytes(connection);
            return new RestResponse(statusCode, mimeType, body);
        } catch (Exception e) {
            throw new RestException(e);
        }
    }


    /**
     * Since the implementations of a POST request and PUT request differ by only one line of code, they are refactored
     * into this private method which is turned wrapped by <code>post()</code> and <code>put()</code>.
     *
     * @param doPost If <code>true</code>, then a POST operation will be performed.  If false, then a PUT.
     * @return The result of the HTTP operation
     * @throws RestException
     */
    private RestResponse postOrPutImpl(final boolean doPost) throws RestException {
        if (urlString == null) {
            throw new RestException("No URL is set");
        }
        try {
            // Initialize HTTP connection, and set any header values
            URLConnection connection;
            if (doPost) {
                connection = initURLConnection(urlString, "POST");
            } else {
                connection = initURLConnection(urlString, "PUT");
            }
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", "UTF-8");

            // If a body payload has been provided, then it takes precedence.  Otherwise, look for any additional
            // parameters to send as form field values.  Parameters sent via the base URL query string are left
            // as-is regardless.
            if (body != null) {
                final OutputStream outputStream = connection.getOutputStream();
                outputStream.write(body);
                outputStream.close();
            } else if (!parameters.isEmpty()) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                final OutputStream outputStream = connection.getOutputStream();
                outputStream.write(parametersToQueryString().getBytes("UTF-8"));
                outputStream.close();
            }

            // Get the resulting status code
            final int statusCode = connectionStatus(connection);
            // Download and parse response
            final String mimeType = connection.getContentType();
            final byte[] body = responseBodyBytes(connection);
            return new RestResponse(statusCode, mimeType, body);
        } catch (IOException e) {
            throw new RestException(e);
        }
    }

    /**
     * <p>This helper method constructs a new <code>HttpURLConnection</code> or <code>HttpsURLConnection</code>,
     * configured with all of the settings that were passed in when first initializing this <code>Rest</code>
     * instance (e.g. timeout thresholds, SSL verification, SSL certificate data).</p>
     *
     * @param urlString The URL to which this connection will be made
     * @param method The applicable request method (e.g. "GET", "POST", etc)
     * @return
     * @throws RestException If the URL cannot be successfully parsed, or if there are errors processing an SSL cert, etc.
     */
    private URLConnection initURLConnection(final String urlString, final String method) throws RestException {
        try {
            final URL url = new URL(urlString);
            final URLConnection connection = url.openConnection();

            // Timeout settings, if applicable
            if (connectTimeoutSeconds != null) {
                connection.setConnectTimeout(connectTimeoutSeconds * 1000);
            }
            if (readTimeoutSeconds != null) {
                connection.setReadTimeout(readTimeoutSeconds * 1000);
            }

            // SSL settings, if applicable
            if (connection instanceof HttpsURLConnection) {
                final HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
                // Cert file supplied

                final SSLContext sslContext = initSSLContext(sslVerification);
                httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());

                // SSL verification disabled
                if (sslVerification != null && !sslVerification.booleanValue()) {
                    httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(final String s, final SSLSession sslSession) {
                            return true;
                        }
                    });
                }
                
                httpsURLConnection.setRequestMethod(method);
            } else if (connection instanceof HttpURLConnection) {
                final HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
                httpURLConnection.setRequestMethod(method);
            } else {
                final String message = "URL string " + urlString + " cannot be parsed as an instance of HttpURLConnection or HttpsURLConnection";
                throw new RestException(message);
            }
            return connection;
        } catch (Exception e) {
            throw new RestException(e);
        }
    }

    /**
     * <p>This helper method is used when a X.509 certificate PEM file has been provided, to configure the HTTPS
     * connection with an in-memory keystore containing that certificate.</p>
     *
     * @return
     * @throws RestException If there are any issues processing the SSL cert
     */
    private SSLContext initSSLContext(Boolean verificatioEnabled) throws RestException {
        
        if (vaultSSLContext != null) {
            // Initialize SSL context once and keep using it.
            return vaultSSLContext;
        }
        
        synchronized (MUTEX) {
            try {
                vaultSSLContext = SSLContext.getInstance("TLS");
                KeyManager[] keyManagers = this.getKeyManagers();
    
                if (verificatioEnabled == null || verificatioEnabled.booleanValue()) {
                    vaultSSLContext.init(keyManagers, this.getTrustManagers(), null);
                } else {
                    vaultSSLContext.init(keyManagers, new TrustManager[] {new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s)
                                throws CertificateException {}
    
                        @Override
                        public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s)
                                throws CertificateException {}
    
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }}, new java.security.SecureRandom());
                }
    
                return vaultSSLContext;
            } catch (Exception e) {
                throw new RestException(e);
            }
        }
    }

    /**
     * <p>This helper method constructs a query string (e.g. <code>param-1=foo&param-2=bar</code>) from any parameters
     * that have been set via the <code>param()</code> method.  Parameters will be sorted by name.</p>
     *
     * @return A url-encoded URL query string
     */
    private String parametersToQueryString() {
        final StringBuilder queryString = new StringBuilder();
        final List<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>(parameters.entrySet());
        for (int index = 0; index < params.size(); index++) {
            if (index > 0) {
                queryString.append('&');
            }
            final String name = params.get(index).getKey();
            final String value = params.get(index).getValue();
            queryString.append(name).append('=').append(value);
        }
        return queryString.toString();
    }

    /**
     * <p>This helper method downloads the body of an HTTP response (e.g. a clob of JSON text) as binary data.</p>
     *
     * @param connection An active HTTP(S) connection
     * @return The body payload, downloaded from the HTTP connection response
     */
    private byte[] responseBodyBytes(final URLConnection connection) {
        try {
            final InputStream inputStream = connection.getInputStream();
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int bytesRead;
            final byte[] bytes = new byte[16384];
            while ((bytesRead = inputStream.read(bytes, 0, bytes.length)) != -1) {
                byteArrayOutputStream.write(bytes, 0, bytesRead);
            }
            byteArrayOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }


    /**
     * <p>This helper method extracts the HTTP(S) status code from a <code>URLConnection</code>, provided
     * that it is an <code>HttpURLConnection</code> or a <code>HttpsUrlConnection</code>.</p>
     *
     * @param connection An active HTTP(S) connection
     * @return
     * @throws IOException
     * @throws RestException
     */
    private int connectionStatus(final URLConnection connection) throws IOException, RestException {
        int statusCode;
        if (connection instanceof HttpsURLConnection) {
            final HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
            statusCode = httpsURLConnection.getResponseCode();
        } else if (connection instanceof HttpURLConnection) {
            final HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            statusCode = httpURLConnection.getResponseCode();
        } else {
            final String className = connection != null ? connection.getClass().getName() : "null";
            throw new RestException("Expecting a URLConnection of type "
                    + HttpURLConnection.class.getName()
                    + " or "
                    + HttpsURLConnection.class.getName()
                    + ", found "
                    + className);
        }
        return statusCode;
    }

    private KeyStore loadKeystore(File keystoreFile, String keystorePassword) throws KeyStoreException,
            FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = null;

        if (keystorePassword != null) {
            password = keystorePassword.toCharArray();
        }

        if (keystoreFile != null && keystoreFile.exists()) {
            try (FileInputStream content = new FileInputStream(keystoreFile)) {
                keyStore.load(content, password);
            }
        } else {
            keyStore.load(null);
        }

        return keyStore;
    }
    
    private KeyManager[] getKeyManagers() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, IOException, UnrecoverableKeyException {
        KeyManager[] keyManagers = null;
        
        if (this.keystoreFile != null && this.keystoreFile.exists()) {
            KeyStore keyStore = loadKeystore(this.keystoreFile, this.keystorePassword);

            KeyManagerFactory keyManFacto = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManFacto.init(keyStore, this.keystorePassword.toCharArray());
            keyManagers = keyManFacto.getKeyManagers();
        }
        
        return keyManagers;
    }

    private TrustManager[] getTrustManagers() throws KeyStoreException, FileNotFoundException, NoSuchAlgorithmException,
            CertificateException, IOException {
        TrustManager[] trustManagers = null;
        KeyStore trustStore = null;
        TrustManagerFactory facto = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        
        if (this.truststore != null && this.truststore.exists()) {
            trustStore = loadKeystore(this.truststore, this.truststorePassword);
        }

        if (this.sslPemUTF8 != null) {
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            final ByteArrayInputStream pem = new ByteArrayInputStream(sslPemUTF8.getBytes("UTF-8"));
            final X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(pem);
            pem.close();

            if (trustStore == null) {
                trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null);
            }
            
            trustStore.setCertificateEntry("caCert", certificate);
        }
        
        if (trustStore != null) {
            facto.init(trustStore);
            trustManagers = facto.getTrustManagers();
        }

        return trustManagers;
    }

    public Rest clone() {
        Rest copy = new Rest();
        
        //copy.urlString = new String(this.urlString);
        //copy.body = Arrays.copyOf(this.body, this.body.length);
        //copy.parameters.putAll(this.parameters);
        //copy.headers.putAll(this.headers);

        copy.connectTimeoutSeconds = this.connectTimeoutSeconds;
        copy.readTimeoutSeconds = this.readTimeoutSeconds;
        copy.sslVerification = this.sslVerification;

        copy.sslPemUTF8 = this.sslPemUTF8;

        copy.keystoreFile = this.keystoreFile;
        copy.truststore = this.truststore;
        copy.keystorePassword = this.keystorePassword;
        copy.truststorePassword = this.truststorePassword;

        copy.vaultSSLContext = this.vaultSSLContext;
        
        return copy;
    }
}

