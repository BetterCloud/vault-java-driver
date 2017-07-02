package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LookupResponse;
import com.bettercloud.vault.rest.RestResponse;
import com.bettercloud.vault.rest.Rest;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>The implementing class for operations on Vault's <code>/v1/auth/*</code> REST endpoints.</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for usage examples.</p>
 */
public class Auth {

    /**
     * Builder-style class for use with {@link #createToken(TokenRequest)}
     *
     * <p>All properties are optional and can be <code>null</code>.</p>
     */
    public static class TokenRequest implements Serializable {
        /**
         * (optional) The ID of the client token. Can only be specified by a root token. Otherwise, the token ID is a randomly generated UUID.
         */
        private UUID id;

        /**
         * (optional) A list of policies for the token. This must be a subset of the policies belonging to the token making the request, unless root. If not specified, defaults to all the policies of the calling token.
         */
        private List<String> polices;

        /**
         * (optional) A map of string to string valued metadata. This is passed through to the audit backends.
         */
        private Map<String, String> meta;

        /**
         * (optional) If true and set by a root caller, the token will not have the parent token of the caller. This creates a token with no parent.
         */
        private Boolean noParent;

        /**
         * (optional) If <code>true</code> the default policy will not be a part of this token's policy set.
         */
        private Boolean noDefaultPolicy;

        /**
         * (optional) The TTL period of the token, provided as "1h", where hour is the largest suffix. If not provided, the token is valid for the default lease TTL, or indefinitely if the root policy is used.
         */
        private String ttl;

        /**
         * (optional) The display name of the token. Defaults to "token".
         */
        private String displayName;

        /**
         * (optional) The maximum uses for the given token. This can be used to create a one-time-token or limited use token. Defaults to 0, which has no limit to the number of uses.
         */
        private Long numUses;

        /**
         * (optional) The role the token will be created with. Default is no role.
         */
        private String role;

        /**
         * {@link #id}
         */
        public TokenRequest id(final UUID id) {
            this.id = id;
            return this;
        }

        /**
         * {@link #polices}
         */
        public TokenRequest polices(final List<String> polices) {
            this.polices = polices;
            return this;
        }

        /**
         * {@link #meta}
         */
        public TokenRequest meta(final Map<String, String> meta) {
            this.meta = meta;
            return this;
        }

        /**
         * {@link #noParent}
         */
        public TokenRequest noParent(final Boolean noParent) {
            this.noParent = noParent;
            return this;
        }

        /**
         * {@link #noDefaultPolicy}
         */
        public TokenRequest noDefaultPolicy(final Boolean noDefaultPolicy) {
            this.noDefaultPolicy = noDefaultPolicy;
            return this;
        }

        /**
         * {@link #ttl}
         */
        public TokenRequest ttl(final String ttl) {
            this.ttl = ttl;
            return this;
        }

        /**
         * {@link #displayName}
         */
        public TokenRequest displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * {@link #numUses}
         */
        public TokenRequest numUses(final Long numUses) {
            this.numUses = numUses;
            return this;
        }

        /**
         * {@link #role}
         */
        public TokenRequest role(final String role) {
            this.role = role;
            return this;
        }


        /**
         * {@link #id}
         */
        public UUID getId() {
            return id;
        }

        /**
         * {@link #polices}
         */
        public List<String> getPolices() {
            return polices;
        }

        /**
         * {@link #meta}
         */
        public Map<String, String> getMeta() {
            return meta;
        }

        /**
         * {@link #noParent}
         */
        public Boolean getNoParent() {
            return noParent;
        }

        /**
         * {@link #noDefaultPolicy}
         */
        public Boolean getNoDefaultPolicy() {
            return noDefaultPolicy;
        }

        /**
         * {@link #ttl}
         */
        public String getTtl() {
            return ttl;
        }

        /**
         * {@link #displayName}
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * {@link #numUses}
         */
        public Long getNumUses() {
            return numUses;
        }

        /**
         * {@link #role}
         */
        public String getRole() {
            return role;
        }
    }

    private final VaultConfig config;

    public Auth(final VaultConfig config) {
        this.config = config;
    }

    /**
     * <p>Operation to create an authentication token.  Relies on another token already being present in
     * the <code>VaultConfig</code> instance.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig(address, rootToken);
     * final Vault vault = new Vault(config);
     * final AuthResponse response = vault.auth().createToken(null, null, null, null, null, "1h", null, null);
     *
     * final String token = response.getAuthClientToken();
     * }</pre>
     * </blockquote>
     *
     * <p>All parameters to this method are optional, and can be <code>null</code>.</p>
     *
     * @param id (optional) The ID of the client token. Can only be specified by a root token. Otherwise, the token ID is a randomly generated UUID.
     * @param policies (optional) A list of policies for the token. This must be a subset of the policies belonging to the token making the request, unless root. If not specified, defaults to all the policies of the calling token.
     * @param meta (optional) A map of string to string valued metadata. This is passed through to the audit backends.
     * @param noParent (optional) If true and set by a root caller, the token will not have the parent token of the caller. This creates a token with no parent.
     * @param noDefaultPolicy (optional) If <code>true</code> the default policy will not be a part of this token's policy set.
     * @param ttl (optional) The TTL period of the token, provided as "1h", where hour is the largest suffix. If not provided, the token is valid for the default lease TTL, or indefinitely if the root policy is used.
     * @param displayName (optional) The display name of the token. Defaults to "token".
     * @param numUses (optional) The maximum uses for the given token. This can be used to create a one-time-token or limited use token. Defaults to 0, which has no limit to the number of uses.
     * @return The auth token
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     * @deprecated Use {@link #createToken(TokenRequest)}
     */
    @Deprecated
    public AuthResponse createToken(
            final UUID id,
            final List<String> policies,
            final Map<String, String> meta,
            final Boolean noParent,
            final Boolean noDefaultPolicy,
            final String ttl,
            final String displayName,
            final Long numUses
    ) throws VaultException {
        return createToken(
                new TokenRequest()
                        .id(id)
                        .polices(policies)
                        .meta(meta)
                        .noParent(noParent)
                        .noDefaultPolicy(noDefaultPolicy)
                        .ttl(ttl)
                        .displayName(displayName)
                        .numUses(numUses));
    }


    /**
     * <p>Operation to create an authentication token.  Relies on another token already being present in
     * the <code>VaultConfig</code> instance.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig(address, rootToken);
     * final Vault vault = new Vault(config);
     * final AuthResponse response = vault.auth().createToken(new TokenRequest().withTtl("1h"));
     *
     * final String token = response.getAuthClientToken();
     * }</pre>
     * </blockquote>     */
    public AuthResponse createToken(TokenRequest tokenRequest) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // Parse parameters to JSON
                final JsonObject jsonObject = Json.object();
              
                if (tokenRequest.id != null) jsonObject.add("id", tokenRequest.id.toString());
                if (tokenRequest.polices != null && !tokenRequest.polices.isEmpty()) {
                    jsonObject.add("policies", Json.array(tokenRequest.polices.toArray(new String[tokenRequest.polices.size()])));//NOPMD
                }
                if (tokenRequest.meta != null && !tokenRequest.meta.isEmpty()) {
                    final JsonObject metaMap = Json.object();
                    for (final Map.Entry<String, String> entry : tokenRequest.meta.entrySet()) {
                        metaMap.add(entry.getKey(), entry.getValue());
                    }
                    jsonObject.add("meta", metaMap);
                }
                if (tokenRequest.noParent != null) jsonObject.add("no_parent", tokenRequest.noParent);
                if (tokenRequest.noDefaultPolicy != null) jsonObject.add("no_default_policy", tokenRequest.noDefaultPolicy);
                if (tokenRequest.ttl != null) jsonObject.add("ttl", tokenRequest.ttl);
                if (tokenRequest.displayName != null) jsonObject.add("display_name", tokenRequest.displayName);
                if (tokenRequest.numUses != null) jsonObject.add("num_uses", tokenRequest.numUses);
                final String requestJson = jsonObject.toString();

                String url = config.getAddress() + "/v1/auth/token/create";
                if (tokenRequest.role != null) url += "/" + tokenRequest.role;

                // HTTP request to Vault
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(url)
                        .header("X-Vault-Token", config.getToken())
                        .body(requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify())
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
                }
                return new AuthResponse(restResponse, retryCount);
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
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }

    /**
     * <p>Basic login operation to authenticate to an app-id backend.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final AuthResponse response = vault.auth().loginByAppID("app-id/login", "app_id", "user_id");
     *
     * final String token = response.getAuthClientToken();
     * }</pre>
     * </blockquote>
     *
     * <strong>NOTE: </strong> As of Vault 0.6.1, Hashicorp has deprecated the App ID authentication backend in
     * favor of AppRole.
     *
     * @param path The path on which the authentication is performed (e.g. <code>auth/app-id/login</code>)
     * @param appId The app-id used for authentication
     * @param userId The user-id used for authentication
     * @return The auth token
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    @Deprecated
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
                        .sslVerification(config.isSslVerify())
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
                }
                return new AuthResponse(restResponse, retryCount);
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
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }

    /**
     * <p>Basic login operation to authenticate to an app-role backend.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final AuthResponse response = vault.auth().loginByAppRole("approle", "9e1aede8-dcc6-a293-8223-f0d824a467ed", "9ff4b26e-6460-834c-b925-a940eddb6880");
     *
     * final String token = response.getAuthClientToken();
     * }</pre>
     * </blockquote>
     *
     * @param path The path on which the authentication is performed (e.g. <code>auth/approle/login</code>)
     * @param roleId The role-id used for authentication
     * @param secretId The secret-id used for authentication
     * @return The auth token
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public AuthResponse loginByAppRole(final String path, final String roleId, final String secretId) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // HTTP request to Vault
                final String requestJson = Json.object().add("role_id", roleId).add("secret_id", secretId).toString();
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/auth/" + path + "/login")
                        .body(requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify())
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
                }
                return new AuthResponse(restResponse, retryCount);
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
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
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
     * final AuthResponse response = vault.auth().loginByUserPass("test", "password");
     *
     * final String token = response.getAuthClientToken();
     * }</pre>
     * </blockquote>
     *
     * @param username The username used for authentication
     * @param password The password used for authentication
     * @return The auth token
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public AuthResponse loginByUserPass(final String username, final String password) throws VaultException {
        return loginByUserPass("userpass", username, password);
    }
    
    /**
     * <p>Basic login operation to authenticate to a Username &amp; Password backend at any path (for example ldap).  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final AuthResponse response = vault.auth().loginByUserPass("ldap", "test", "password");
     *
     * final String token = response.getAuthClientToken();
     * }</pre>
     * </blockquote>
     *
     * @param path The authentication backend mount path
     * @param username The username used for authentication
     * @param password The password used for authentication
     * @return The auth token
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public AuthResponse loginByUserPass(final String path, final String username, final String password) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // HTTP request to Vault
                final String requestJson = Json.object().add("password", password).toString();
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/auth/" + path + "/login/" + username)
                        .body(requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify())
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
                }
                return new AuthResponse(restResponse, retryCount);
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
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }

     /**
     * <p>Basic login operation to authenticate to an github backend.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final AuthResponse response = vault.auth().loginByGithub("githubToken");
     *
     * final String token = response.getAuthClientToken();
     * }</pre>
     * </blockquote>
     *
     * @param githubToken The app-id used for authentication
     * @return The auth token
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public AuthResponse loginByGithub(final String githubToken) throws VaultException {

        // TODO:  Add (optional?) integration test coverage

        int retryCount = 0;
        while (true) {
            try {
                // HTTP request to Vault
                final String requestJson = Json.object().add("token", githubToken).toString();
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/auth/github/login")
                        .body(requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify())
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
                }
                return new AuthResponse(restResponse, retryCount);
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
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }

    /**
     * <p>Renews the lease associated with the calling token.  This version of the method tells Vault to use the
     * default lifespan for the new lease.</p>
     *
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
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
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
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
                        .sslVerification(config.isSslVerify())
                        .post();
                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
                if (!mimeType.equals("application/json")) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
                }
                return new AuthResponse(restResponse, retryCount);
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
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }

    /**
     * <p>Returns information about the current client token.</p>
     *
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LookupResponse lookupSelf() throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                // HTTP request to Vault
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/auth/token/lookup-self")
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify())
                        .get();
                // Validate restResponse
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                final String mimeType = restResponse.getMimeType();
                if (mimeType == null || !"application/json".equals(mimeType)) {
                    throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
                }
                return new LookupResponse(restResponse, retryCount);
            } catch (Exception e) {
                // If there are retries to perform, then pause for the configured interval and then execute the loop again...
                if (retryCount < config.getMaxRetries()) {
                    retryCount++;
                    try {
                        final int retryIntervalMilliseconds = config.getRetryIntervalMilliseconds();
                        Thread.sleep(retryIntervalMilliseconds);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace(); //NOPMD
                    }
                } else if (e instanceof VaultException) { //NOPMD
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }

}
