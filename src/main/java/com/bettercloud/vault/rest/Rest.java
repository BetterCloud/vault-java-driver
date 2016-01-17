package com.bettercloud.vault.rest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Rest {

    private String urlString;
    private RequestType requestType;
    private Map<String, String> parameters = new HashMap<String, String>();
    private Map<String, String> headers = new HashMap<String, String>();

    public final Rest url(final String urlString) {
        this.urlString = urlString;
        return this;
    }

    public final Rest parameter(final String name, final String value) {
        this.parameters.put(name, value);
        return this;
    }

    public final Rest type(final RequestType requestType) {
        this.requestType = requestType;
        return this;
    }

    public final Rest header(final String name, final String value) {
        this.headers.put(name, value);
        return this;
    }

    public final Response execute() throws RestException {
        if (urlString == null) {
            throw new RestException("No URL is set");
        }
        if (requestType == null || requestType == RequestType.GET) {
            return doGet();
        } else {
            return doPost();
        }
    }

    private Response doGet() throws RestException {
        if (!parameters.isEmpty()) {
            if (urlString.indexOf('?') == -1) {
                urlString = urlString + "?" + parametersToQueryString();
            } else {
                urlString = urlString + "&" + parametersToQueryString();
            }
        }
        try {
            final URL url = new URL(urlString);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder body = new StringBuilder();
            String line;
            while ( (line = reader.readLine()) != null ) {
                body.append(line);
            }
            reader.close();

            final int statusCode = connection.getResponseCode();
            final String mimeType = connection.getContentType();
            return new Response(statusCode, mimeType, body.toString());
        } catch (IOException e) {
            throw new RestException(e);
        }
    }

    private Response doPost() throws RestException {
        try {
            final URL url = new URL(urlString);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (!parameters.isEmpty()) {
                final DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.writeBytes(parametersToQueryString());
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder body = new StringBuilder();
            String line;
            while ( (line = reader.readLine()) != null ) {
                body.append(line);
            }
            reader.close();

            final int statusCode = connection.getResponseCode();
            final String mimeType = connection.getContentType();
            return new Response(statusCode, mimeType, body.toString());
        } catch (IOException e) {
            throw new RestException(e);
        }
    }

    private String parametersToQueryString() {
        final StringBuilder queryString = new StringBuilder();
        final List<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>(parameters.entrySet());
        for (int index = 0; index < params.size(); index++) {
            if (index > 0) {
                queryString.append("&");
            }
            queryString.append(params.get(index).getKey()).append("=").append(params.get(index).getValue());
        }
        return queryString.toString();
    }

}

