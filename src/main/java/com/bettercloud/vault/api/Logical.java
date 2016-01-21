package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.rest.Response;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class Logical {

    private final VaultConfig config;

    public Logical(final VaultConfig config) {
        this.config = config;
    }

    /**
     * Basic read operation to retrieve a secret.
     *
     * TODO: Perhaps devise a complex return type, capturing the value read along with all metadata, rather than just returning the value as a plain string.
     *
     * @param path The path on which the secret is stored (e.g. <code>secret/hello</code>)
     * @return The secret value
     * @throws VaultException
     */
    public String read(final String path) throws VaultException {
        final Map<Class<? extends Exception>, Integer> retryCounts = new HashMap<Class<? extends Exception>, Integer>();
        while (true) {
            try {
                // HTTP request to Vault
                final Response response = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/" + path)
                        .header("X-Vault-Token", config.getToken())
                        .get();

                // Validate response
                if (response.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + response.getStatus());
                }
                final String mimeType = response.getMimeType() == null ? "null" : response.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType);
                }
                String jsonString;
                try {
                    jsonString = new String(response.getBody(), "UTF-8");//NOPMD
                } catch (UnsupportedEncodingException e) {
                    throw new VaultException(e);
                }

                // Parse JSON
                final JsonObject jsonObject = Json.parse(jsonString).asObject();
                return jsonObject.get("data").asObject().getString("value", "");
            } catch (RestException e) {
                final int maxRetries = config.getMaxRetriesForException(e.getClass());
                final int currentRetries = retryCounts.get(e.getClass()) == null ? 0 : retryCounts.get(e.getClass());
                if (currentRetries + 1 < maxRetries) {
                    retryCounts.put(e.getClass(), currentRetries + 1);
                    final int retryInterval = config.getRetryIntervalForException(e.getClass());
                    try {
                        Thread.sleep(retryInterval * 1000);
                        continue;
                    } catch (InterruptedException e1) {
                        throw new VaultException(e1);
                    }
                }
                throw new VaultException(e);
            }
        }
    }

    /**
     * Basic operation to store a secret.
     *
     * TODO: Determine the best return type for this method
     *
     * @param path The path on which the secret is to be stored (e.g. <code>secret/hello</code>)
     * @param value The secret value to be stored
     * @throws VaultException
     */
    public void write(final String path, final String value) throws VaultException {
        try {
            final Response response = new Rest()
                    .url(config.getAddress() + "/v1/" + path)
                    .body(Json.object().add("value", value).toString().getBytes("UTF-8"))
                    .header("X-Vault-Token", config.getToken())
                    .post();
            // response.getStatus() == 204 (success)
        } catch (RestException e) {
            throw new VaultException(e);
        } catch (UnsupportedEncodingException e) {
            throw new VaultException(e);
        }

    }

}
