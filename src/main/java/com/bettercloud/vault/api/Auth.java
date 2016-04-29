package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.rest.RestResponse;
import com.bettercloud.vault.rest.Rest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>The implementing class for operations on Vault's <code>/v1/auth/*</code> REST endpoints.</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for usage examples.</p>
 */
public class Auth {

    private final VaultConfig config;

    public Auth(final VaultConfig config) {
        this.config = config;
    }

    /**
     * <p>Basic login operation to authenticate to an app-id backend.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final AuthResponse response = vault.auth().loginByAppID("app-id/login", "app_id", "user_id");
     *
     * final String token = response.getAuthClientToken());
     * }</pre>
     * </blockquote>
     *
     * @param path The path on which the authentication is performed (e.g. <code>auth/app-id/login</code>)
     * @param appId The app-id used for authentication
     * @param userId The user-id used for authentication
     * @return The auth token
     * @throws VaultException
     */
    public AuthResponse loginByAppID(final String path, final String appId, final String userId) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // HTTP request to Vault
                final String requestJson = Json.object().add("app_id", appId).add("user_id", userId).toString();
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/auth/" + path)
                        .body(requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType);
                }
                return buildAuthResponse(restResponse, retryCount);
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
     * <p>Basic login operation to authenticate to a Username &amp; Password backend.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final AuthResponse response = vault.auth().loginByUsernamePassword("userpass/login/test", "password");
     *
     * final String token = response.getAuthClientToken());
     * }</pre>
     * </blockquote>
     *
     * @param path The path on which the authentication is performed (e.g. <code>auth/userpass/login/username</code>)
     * @param password The password used for authentication
     * @return The auth token
     * @throws VaultException
     */
    public AuthResponse loginByUsernamePassword(final String path, final String password) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // HTTP request to Vault
                final String requestJson = Json.object().add("password", password).toString();
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/auth/" + path)
                        .body(requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType);
                }
                return buildAuthResponse(restResponse, retryCount);
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
     * <p>Renews the lease associated with the calling token.  This version of the method tells Vault to use the
     * default lifespan for the new lease.</p>
     *
     * @return
     * @throws VaultException
     */
    public AuthResponse renewSelf() throws VaultException {
        return renewSelf(-1);
    }

    /**
     * <p>Renews the lease associated with the calling token.  This version of the method accepts a parameter to
     * explicitly declare how long the new lease period should be (in seconds).  The Vault documentation suggests
     * that this value may be ignored, however.</p>
     *
     * @param increment The number of seconds requested for the new lease lifespan
     * @throws VaultException
     */
    public AuthResponse renewSelf(final long increment) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // HTTP request to Vault
                final String requestJson = Json.object().add("increment", increment).toString();
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/auth/token/renew-self")
                        .header("X-Vault-Token", config.getToken())
                        .body(increment < 0 ? null : requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .post();
                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType);
                }
                return buildAuthResponse(restResponse, retryCount);
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

    private AuthResponse buildAuthResponse(final RestResponse restResponse, final int retries)
            throws UnsupportedEncodingException {
        final AuthResponse authResponse = new AuthResponse(restResponse, retries);

        final String responseJson = new String(restResponse.getBody(), "UTF-8");
        final JsonObject jsonObject = Json.parse(responseJson).asObject();
        final JsonObject authJsonObject = jsonObject.get("auth").asObject();

        authResponse.setAuthLeaseDuration(authJsonObject.getInt("lease_duration",0));
        authResponse.setAuthRenewable(authJsonObject.getBoolean("renewable",false));
        authResponse.setAppId(authJsonObject.get("metadata").asObject().getString("app-id",""));
        authResponse.setUserId(authJsonObject.get("metadata").asObject().getString("user-id",""));
        authResponse.setUsername(authJsonObject.get("metadata").asObject().getString("username",""));
        authResponse.setAuthClientToken(authJsonObject.getString("client_token",""));

        final JsonArray authPoliciesJsonArray = authJsonObject.get("policies").asArray();
        final List<String> authPolicies = new ArrayList<String>();
        for (final JsonValue authPolicy : authPoliciesJsonArray) {
            authPolicies.add(authPolicy.asString());
        }
        authResponse.setAuthPolicies(authPolicies);

        return authResponse;
    }
}
