package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.RestResponse;
import com.bettercloud.vault.rest.Rest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class Logical {

    private final VaultConfig config;

    public Logical(final VaultConfig config) {
        this.config = config;
    }

    /**
     * <p>Basic read operation to retrieve a secret.  A single secret key can map to multiple name-value pairs,
     * which can be retrieved from the response object.  E.g.:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final LogicalResponse response = vault.logical().read("secret/hello");
     *
     * final String value = response.getData().get("value");
     * final String otherValue = response.getData().get("other_value");
     * }</pre>
     * </blockquote>
     *
     * @param path The Vault key value from which to read (e.g. <code>secret/hello</code>)
     * @return
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code, invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public LogicalResponse read(final String path) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // Make an HTTP request to Vault
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/" + path)
                        .header("X-Vault-Token", config.getToken())
                        .get();

                // Validate response
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType);
                }
                String jsonString;
                try {
                    jsonString = new String(restResponse.getBody(), "UTF-8");//NOPMD
                } catch (UnsupportedEncodingException e) {
                    throw new VaultException(e);
                }

                // Parse JSON
                final Map<String, String> data = new HashMap<String, String>();//NOPMD
                for (final JsonObject.Member member : Json.parse(jsonString).asObject().get("data").asObject()) {
                    data.put(member.getName(), member.getValue().asString());
                }
                return new LogicalResponse(restResponse, retryCount, data);
            } catch (Exception e) {
                // If there are retries to perform, then pause for the configured interval and then execute the loop again...
                if (retryCount < config.getMaxRetries()) {
                    retryCount++;
                    try {
                        final int retryIntervalMilliseconds = config.getRetryIntervalMilliseconds();
                        Thread.sleep(retryIntervalMilliseconds);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    // ... otherwise, give up.
                    throw new VaultException(e);
                }
            }
        }
    }

    /**
     * <p>Basic operation to store secrets.  Multiple name value pairs can be stored under the same secret key.
     * E.g.:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final Map<String, String> nameValuePairs = new HashMap<String, String>();
     * nameValuePairs.put("value", "foo");
     * nameValuePairs.put("other_value", "bar");
     *
     * final LogicalResponse response = vault.logical().write("secret/hello", nameValuePairs);
     * }</pre>
     * </blockquote>
     *
     * @param path The Vault key value to which to write (e.g. <code>secret/hello</code>)
     * @param nameValuePairs Secret name and value pairs to store under this Vault key
     * @throws VaultException
     */
    public LogicalResponse write(final String path, final Map<String, String> nameValuePairs) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                JsonObject requestJson = Json.object();
                for (final Map.Entry<String, String> pair : nameValuePairs.entrySet()) {
                    requestJson = requestJson.add(pair.getKey(), pair.getValue());
                }

                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/" + path)
                        .body(requestJson.toString().getBytes("UTF-8"))
                        .header("X-Vault-Token", config.getToken())
                        .post();
                if (restResponse.getStatus() != 204) {
                    throw new VaultException("Expecting HTTP status 204, but instead receiving " + restResponse.getStatus());
                }
                return new LogicalResponse(null, retryCount);
            } catch (Exception e) {
                // If there are retries to perform, then pause for the configured interval and then execute the loop again...
                if (retryCount < config.getMaxRetries()) {
                    retryCount++;
                    try {
                        final int retryIntervalMilliseconds = config.getRetryIntervalMilliseconds();
                        Thread.sleep(retryIntervalMilliseconds);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    // ... otherwise, give up.
                    throw new VaultException(e);
                }
            }
        }
    }

}
