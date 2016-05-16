package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestException;
import com.bettercloud.vault.rest.RestResponse;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The implementing class for operations on Vault's <code>/v1/sys/*</code> REST endpoints.</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for usage examples.</p>
 */
public class Sys {
    private final String sys = "sys";
    private final VaultConfig config;

    public Sys(final VaultConfig config) {
        this.config = config;
    }

    /**
     * <p>Returns the health status of Vault. This matches the semantics of a Consul HTTP
     * health check and provides a simple way to monitor the health of a Vault instance.</p>
     *
     * @see <a href="https://www.vaultproject.io/docs/http/sys-health.html">https://www.vaultproject.io/docs/http/sys-health.html</a>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig(address, rootToken);
     * final Vault vault = new Vault(config);
     * final HashMap<String, String> params = new HashMap<>();
     * params.put("standbyok", "true");
     *
     * final Map<String, String> response = vault.sys().health(params);
     *
     * final String sealed = response.get("sealed");
     * }</pre>
     * </blockquote>
     *
     * @param params the optional query paramaters
     * @return
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code, invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public Map<String, String> health(Map<String, String> params) throws VaultException {
        String path = sys + "/health";
        return get(path, params);
    }

    /**
     * <p>Returns the health status of Vault. This matches the semantics of a Consul HTTP
     * health check and provides a simple way to monitor the health of a Vault instance.</p>
     *
     * @see <a href="https://www.vaultproject.io/docs/http/sys-health.html">https://www.vaultproject.io/docs/http/sys-health.html</a>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig(address, rootToken);
     * final Vault vault = new Vault(config);
     *
     * final Map<String, String> response = vault.sys().health();
     *
     * final String token = response.get("sealed");
     * }</pre>
     * </blockquote>
     *
     * @return
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code, invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public Map<String, String> health() throws VaultException {
        return health(new HashMap<String, String>());
    }

    private Map<String, String> get(String path, Map<String, String> params) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // Make an HTTP request to Vault
                Rest rest = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/" + path)
                    .header("X-Vault-Token", config.getToken())
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslPemUTF8(config.getSslPemUTF8())
                    .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null);

                // add params
                for(Map.Entry<String, String> entry : params.entrySet()) {
                    rest.parameter(entry.getKey(), entry.getValue());
                }

                final RestResponse restResponse = rest.get();

                // Validate response
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }

                return parseSysResponse(restResponse);
            } catch (RuntimeException | VaultException | RestException e) {
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

    private Map<String, String> parseSysResponse(final RestResponse restResponse) throws VaultException {
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
        final Map<String, String> response = new HashMap<String, String>();//NOPMD
        for (final JsonObject.Member member : Json.parse(jsonString).asObject()) {
            final JsonValue jsonValue = member.getValue();
            if (jsonValue == null || jsonValue.isNull()) {
                continue;
            } else if (jsonValue.isString()) {
                response.put(member.getName(), jsonValue.asString());
            } else {
                response.put(member.getName(), jsonValue.toString());
            }
        }
        return response;
    }
}
