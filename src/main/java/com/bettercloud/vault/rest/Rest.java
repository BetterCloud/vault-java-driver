package com.bettercloud.vault.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>A simple client for issuing HTTP requests.  Supports the HTTP verbs:</p>
 * <ul>
 *     <li>GET</li>
 *     <li>POST</li>
 *     <li>PUT</li>
 *     <li>DELETE</li>
 * </ul>
 *
 * <p><code>Rest</code> uses the Builder pattern to provide a basic DSL for
 * usage.  Methods for configuring an HTTP request (i.e. <code>url()</code>,
 * <code>parameter()</code>, and <code>header()</code>) are designed to be
 * chained together, while methods corresponding to the HTTP verbs are
 * terminating operations:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final Response response = new Rest()
 *                              .url("https://httpbin.org/get")
 *                              .header("header-1", "foo")
 *                              .header("header-2", "bar")
 *                              .parameter("param-1", "up")
 *                              .parameter("param-2", "down")
 *                              .get();
 * }</pre>
 * </blockquote>
 *
 * <p>Header and parameter names and values are url-encoded by the Rest client
 * prior to sending the request.  The URL string should be url-encoded by you
 * (if necessary) prior to passing it.</p>
 */
public final class Rest {

    private String urlString;
    private byte[] body;
    private final Map<String, String> parameters = new TreeMap<String, String>();
    private final Map<String, String> headers = new TreeMap<String, String>();

    /**
     * <p>Sets the base URL to which the HTTP request will be sent.  The URL may or may not
     * include query parameters (e.g. <code>http://httpbin.org/get?param-1=foo</code>).</p>
     *
     * <p>Depending on which HTTP verb is ultimately used, than any additional parameters
     * set via the <code>parameters()</code> method may be appending to this URL.</p>
     *
     * <p>Either way, the responsibility for any url-encoding of this base URL string
     * belongs to the caller.</p>
     *
     * @param urlString A URL string, with any necessary url-encoding already applied
     * @return The <code>Rest</code> instance itself
     */
    public Rest url(final String urlString) {
        this.urlString = urlString;
        return this;
    }

    /**
     * TODO: Document, and add unit test coverage.
     *
     * @param body The payload to send with a POST or PUT request (e.g. a JSON string)
     * @return The <code>Rest</code> instance itself
     */
    public Rest body(final byte[] body) {
        this.body = body;
        return this;
    }

    /**
     * <p>Adds a parameter to be sent with the HTTP request.</p>
     *
     * <p>Depending on which HTTP verb is ultimately used, this parameter may either be appended
     * to the URL or else posted with the request body.  Either way, both the parameter name and
     * value will be automatically url-encoded by the Rest client.</p>
     *
     * <p>This method may be chained together repeatedly, to pass multiple parameters with a
     * request.  When the request is ultimately sent, the parameters will be sorted by their
     * names.</p>
     *
     * @param name The raw parameter name (not url-encoded)
     * @param value The raw parameter value (not url-encoded)
     * @return The <code>Rest</code> instance itself
     * @throws RestException
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
     * <p>This method may be chained together repeatedly, to pass multiple headers with a
     * request.  When the request is ultimately sent, the headers will be sorted by their names.</p>
     *
     * @param name The raw header name (not url-encoded)
     * @param value The raw header value (not url-encoded)
     * @return The <code>Rest</code> instance itself
     * @throws RestException
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
     * Executes an HTTP GET request with the settings already configured.  Parameters and
     * headers are optional, but a <code>RestException</code> will be thrown if the caller
     * has not first set a base URL with the <code>url()</code> method.
     *
     * @return The result of the HTTP operation
     * @throws RestException
     */
    public Response get() throws RestException {
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
            // Initialize HTTP connection, and set any header values
            final URL url = new URL(urlString);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Download and parse response
            final int statusCode = connection.getResponseCode();
            final String mimeType = connection.getContentType();
            final byte[] body = responseBodyBytes(connection);
            return new Response(statusCode, mimeType, body);
        } catch (IOException e) {
            throw new RestException(e);
        }
    }

    /**
     * Executes an HTTP POST request with the settings already configured.  Parameters and
     * headers are optional, but a <code>RestException</code> will be thrown if the caller
     * has not first set a base URL with the <code>url()</code> method.
     *
     * @return The result of the HTTP operation
     * @throws RestException
     */
    public Response post() throws RestException {
        return postOrPutImpl(true);
    }

    /**
     * Executes an HTTP PUT request with the settings already configured.  Parameters and
     * headers are optional, but a <code>RestException</code> will be thrown if the caller
     * has not first set a base URL with the <code>url()</code> method.
     *
     * @return The result of the HTTP operation
     * @throws RestException
     */
    public Response put() throws RestException {
        return postOrPutImpl(false);
    }

    /**
     * Executes an HTTP DELETE request with the settings already configured.  Parameters and
     * headers are optional, but a <code>RestException</code> will be thrown if the caller
     * has not first set a base URL with the <code>url()</code> method.
     *
     * @return The result of the HTTP operation
     * @throws RestException
     */
    public Response delete() throws RestException {
        // TODO: Implement
        throw new UnsupportedOperationException();
    }


    /**
     * Since the implementations of a POST request and PUT request differ by only one line
     * of code, they are refactored into this private method which is turned wrapped by
     * <code>post()</code> and <code>put()</code>.
     *
     * @param doPost If <code>true</code>, then a POST operation will be performed.  If false, then a PUT.
     * @return The result of the HTTP operation
     * @throws RestException
     */
    private Response postOrPutImpl(final boolean doPost) throws RestException {
        if (urlString == null) {
            throw new RestException("No URL is set");
        }
        try {
            // Initialize HTTP connection, and set any header values
            final URL url = new URL(urlString);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (doPost) {
                connection.setRequestMethod("POST");
            } else {
                connection.setRequestMethod("PUT");
            }
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            // TODO: This needs a bit more thought.  Do we even need to support at all the possibility of body params for POST or PUT requests?
            if (body != null) {
                final OutputStream outputStream = connection.getOutputStream();
                outputStream.write(body);
                outputStream.close();
            } else if (!parameters.isEmpty()) {
                // Write any parameters in the request body (NOTE: There can *also* be parameters set via the URL
                // query string.  This logic does not append or remove anything from the request URL).
                final OutputStream outputStream = connection.getOutputStream();
                outputStream.write(parametersToQueryString().getBytes("UTF-8"));
                outputStream.close();
            }

            // Download and parse response
            final int statusCode = connection.getResponseCode();
            final String mimeType = connection.getContentType();
            final byte[] body = responseBodyBytes(connection);
            return new Response(statusCode, mimeType, body);
        } catch (IOException e) {
            throw new RestException(e);
        }
    }

    /**
     * <p>This helper method constructs a query string (e.g. <code>param-1=foo&param-2=bar</code>)
     * from any parameters that have been set via the <code>param()</code> method.  Parameters
     * will be sorted by name.</p>
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
     * <p>This helper method downloads the body of an HTTP response (e.g. a clob of JSON
     * text) as binary data.</p>
     *
     * @param connection An active HTTP connection
     * @return The body payload, downloaded from the HTTP connection response
     */
    private byte[] responseBodyBytes(final HttpURLConnection connection) {
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

}

