package com.bettercloud.vault.api.database;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.DatabaseResponse;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>The implementing class for operations on Vault's database backend.</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for usage examples.</p>
 */
public class Database {

    private final VaultConfig config;
    private final String mountPath;
    private String nameSpace;

    public Database withNameSpace(final String nameSpace) {
        this.nameSpace = nameSpace;
        return this;
    }

    /**
     * Constructor for use when the Database backend is mounted on the default path (i.e. <code>/v1/database</code>).
     *
     * @param config A container for the configuration settings needed to initialize a <code>Vault</code> driver instance
     */
    public Database(final VaultConfig config) {
        this(config, "database");
    }

    /**
     * Constructor for use when the Database backend is mounted on some non-default custom path (e.g. <code>/v1/db123</code>).
     *
     * @param config    A container for the configuration settings needed to initialize a <code>Vault</code> driver instance
     * @param mountPath The path on which your Vault Database backend is mounted, without the <code>/v1/</code> prefix (e.g. <code>"root-ca"</code>)
     */
    public Database(final VaultConfig config, final String mountPath) {
        this.config = config;
        this.mountPath = mountPath;
        if (this.config.getNameSpace() != null && !this.config.getNameSpace().isEmpty()) {
            this.nameSpace = this.config.getNameSpace();
        }
    }

    /**
     * <p>Operation to create or update an role using the Database Secret engine.
     * Relies on an authentication token being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>This version of the method accepts a <code>DatabaseRoleOptions</code> parameter, containing optional settings
     * for the role creation operation.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final DatabaseRoleOptions options = new DatabaseRoleOptions()
     *                              .dbName("test")
     *                              .maxTtl("9h");
     * final DatabaseResponse response = vault.database().createOrUpdateRole("testRole", options);
     *
     * assertEquals(204, response.getRestResponse().getStatus());
     * }</pre>
     * </blockquote>
     *
     * @param roleName A name for the role to be created or updated
     * @param options  Optional settings for the role to be created or updated (e.g. db_name, ttl, etc)
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public DatabaseResponse createOrUpdateRole(final String roleName, final DatabaseRoleOptions options) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                final String requestJson = roleOptionsToJson(options);

                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/roles/%s", config.getAddress(), this.mountPath, roleName))
                        .header("X-Vault-Token", config.getToken())
                        .header("X-Vault-Namespace", this.nameSpace)
                        .body(requestJson.getBytes(StandardCharsets.UTF_8))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 204) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                return new DatabaseResponse(restResponse, retryCount);
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
     * <p>Operation to retrieve an role using the Database backend.  Relies on an authentication token being present in
     * the <code>VaultConfig</code> instance.</p>
     *
     * <p>The role information will be populated in the <code>roleOptions</code> field of the <code>DatabaseResponse</code>
     * return value.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     * final DatabaseResponse response = vault.database().getRole("testRole");
     *
     * final RoleOptions details = response.getRoleOptions();
     * }</pre>
     * </blockquote>
     *
     * @param roleName The name of the role to retrieve
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public DatabaseResponse getRole(final String roleName) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/roles/%s", config.getAddress(), this.mountPath, roleName))
                        .header("X-Vault-Token", config.getToken())
                        .header("X-Vault-Namespace", this.nameSpace)
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .get();

                // Validate response
                if (restResponse.getStatus() != 200 && restResponse.getStatus() != 404) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                return new DatabaseResponse(restResponse, retryCount);
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
     * <p>Operation to revike  a certificate in the vault using the Database backend.
     * Relies on an authentication token being present in
     * the <code>VaultConfig</code> instance.</p>
     *
     * <p>A successful operation will return a 204 HTTP status.  A <code>VaultException</code> will be thrown if
     * the role does not exist, or if any other problem occurs.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final DatabaseResponse response = vault.database().revoke("serialnumber");
     * assertEquals(204, response.getRestResponse().getStatus();
     * }</pre>
     * </blockquote>
     *
     * @param serialNumber The name of the role to delete
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public DatabaseResponse revoke(final String serialNumber) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            JsonObject jsonObject = new JsonObject();
            if (serialNumber != null) {
                jsonObject.add("serial_number", serialNumber);
            }
            final String requestJson = jsonObject.toString();
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/revoke", config.getAddress(), this.mountPath))
                        .header("X-Vault-Token", config.getToken())
                        .header("X-Vault-Namespace", this.nameSpace)
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .body(requestJson.getBytes(StandardCharsets.UTF_8))
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .post();

                // Validate response
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                return new DatabaseResponse(restResponse, retryCount);
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
     * <p>Operation to delete an role using the Database backend.  Relies on an authentication token being present in
     * the <code>VaultConfig</code> instance.</p>
     *
     * <p>A successful operation will return a 204 HTTP status.  A <code>VaultException</code> will be thrown if
     * the role does not exist, or if any other problem occurs.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final DatabaseResponse response = vault.database().deleteRole("testRole");
     * assertEquals(204, response.getRestResponse().getStatus();
     * }</pre>
     * </blockquote>
     *
     * @param roleName The name of the role to delete
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public DatabaseResponse deleteRole(final String roleName) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/roles/%s", config.getAddress(), this.mountPath, roleName))
                        .header("X-Vault-Token", config.getToken())
                        .header("X-Vault-Namespace", this.nameSpace)
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .delete();

                // Validate response
                if (restResponse.getStatus() != 204) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                return new DatabaseResponse(restResponse, retryCount);
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
     * <p>Operation to generate a new set of credentials using the Database backend.
     *
     * <p>A successful operation will return a 204 HTTP status.  A <code>VaultException</code> will be thrown if
     * the role does not exist, or if any other problem occurs.  Credential information will be populated in the
     * <code>credential</code> field of the <code>DatabaseResponse</code> return value.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final DatabaseResponse response = vault.database().creds("testRole");
     * assertEquals(204, response.getRestResponse().getStatus();
     * }</pre>
     * </blockquote>
     *
     * @param roleName The role for which to retrieve credentials
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public DatabaseResponse creds(final String roleName) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/creds/%s", config.getAddress(), this.mountPath, roleName))
                        .header("X-Vault-Token", config.getToken())
                        .header("X-Vault-Namespace", this.nameSpace)
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .get();

                // Validate response
                if (restResponse.getStatus() != 200 && restResponse.getStatus() != 404) {
                    String body = restResponse.getBody() != null ? new String(restResponse.getBody()) : "(no body)";
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus() + " " + body, restResponse.getStatus());
                }

                return new DatabaseResponse(restResponse, retryCount);
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

    private String roleOptionsToJson(final DatabaseRoleOptions options) {
        final JsonObject jsonObject = Json.object();

        if (options != null) {
            addJsonFieldIfNotNull(jsonObject, "db_name", options.getDbName());
            addJsonFieldIfNotNull(jsonObject, "default_ttl", options.getDefaultTtl());
            addJsonFieldIfNotNull(jsonObject, "max_ttl", options.getMaxTtl());
            addJsonFieldIfNotNull(jsonObject, "creation_statements", joinList(options.getCreationStatements()));
            addJsonFieldIfNotNull(jsonObject, "revocation_statements", joinList(options.getRevocationStatements()));
            addJsonFieldIfNotNull(jsonObject, "rollback_statements", joinList(options.getRollbackStatements()));
            addJsonFieldIfNotNull(jsonObject, "renew_statements", joinList(options.getRenewStatements()));
        }

        return jsonObject.toString();
    }

    private String joinList(List<String> data) {
        String result = "";

        if (data != null && !data.isEmpty()) {
            result = String.join(",", data);
        }

        return result;
    }

    private JsonObject addJsonFieldIfNotNull(final JsonObject jsonObject, final String name, final Object value) {
        if (value == null) {
            return jsonObject;
        }
        if (value instanceof String) {
            jsonObject.add(name, (String) value);
        } else if (value instanceof Boolean) {
            jsonObject.add(name, (Boolean) value);
        } else if (value instanceof Long) {
            jsonObject.add(name, (Long) value);
        }

        return jsonObject;
    }
}
