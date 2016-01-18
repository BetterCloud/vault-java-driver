package com.bettercloud.vault.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Rest {

    private String urlString;
    private final Map<String, String> parameters = new TreeMap<String, String>();
    private final Map<String, String> headers = new TreeMap<String, String>();

    public Rest url(final String urlString) {
        this.urlString = urlString;
        return this;
    }

    public Rest parameter(final String name, final String value) throws RestException {
        try {
            this.parameters.put(URLEncoder.encode(name, "UTF-8"), URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RestException(e);
        }
        return this;
    }

    public Rest header(final String name, final String value) throws RestException {
        try {
            this.headers.put(URLEncoder.encode(name, "UTF-8"), URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RestException(e);
        }
        return this;
    }

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
            final String body = responseBodyToString(connection);
            return new Response(statusCode, mimeType, body);
        } catch (IOException e) {
            throw new RestException(e);
        }
    }

    public Response post() throws RestException {
        return postOrPutImpl(true);
    }

    public Response put() throws RestException {
        return postOrPutImpl(false);
    }

    private Response postOrPutImpl(boolean doPost) throws RestException {
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

            if (!parameters.isEmpty()) {
                // Write any parameters in the request body (NOTE: There can *also* be parameters set via the URL
                // query string.  This logic does not append or remove anything from the request URL).
                connection.setDoOutput(true);
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(parametersToQueryString().getBytes("UTF-8"));
                outputStream.close();
            }

            // Download and parse response
            final int statusCode = connection.getResponseCode();
            final String mimeType = connection.getContentType();
            final String body = responseBodyToString(connection);
            return new Response(statusCode, mimeType, body);
        } catch (IOException e) {
            throw new RestException(e);
        }
    }

    public Response delete() throws RestException {
        // TODO: Implement
        throw new UnsupportedOperationException();
    }


    private String parametersToQueryString() throws UnsupportedEncodingException {
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

    private String responseBodyToString(final HttpURLConnection connection) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        final StringBuilder body = new StringBuilder();
        String line;
        while ( (line = reader.readLine()) != null ) {
            body.append(line);
        }
        reader.close();
        return body.toString();
    }

}

