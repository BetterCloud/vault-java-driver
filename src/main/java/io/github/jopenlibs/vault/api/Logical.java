package io.github.jopenlibs.vault.api;

import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.json.JsonValue;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.rest.Rest;
import io.github.jopenlibs.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static io.github.jopenlibs.vault.api.LogicalUtilities.adjustPathForDelete;
import static io.github.jopenlibs.vault.api.LogicalUtilities.adjustPathForList;
import static io.github.jopenlibs.vault.api.LogicalUtilities.adjustPathForReadOrWrite;
import static io.github.jopenlibs.vault.api.LogicalUtilities.adjustPathForVersionDelete;
import static io.github.jopenlibs.vault.api.LogicalUtilities.adjustPathForVersionDestroy;
import static io.github.jopenlibs.vault.api.LogicalUtilities.adjustPathForVersionUnDelete;
import static io.github.jopenlibs.vault.api.LogicalUtilities.jsonObjectToWriteFromEngineVersion;

/**
 * <p>The implementing class for Vault's core/logical operations (e.g. read, write).</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of
 * <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for
 * usage examples.</p>
 */
public class Logical extends OperationsBase {

    private String nameSpace;

    public enum logicalOperations {authentication, deleteV1, deleteV2, destroy, listV1, listV2, readV1, readV2, writeV1, writeV2, unDelete, mount}

    public Logical(final VaultConfig config) {
        super(config);

        if (this.config.getNameSpace() != null && !this.config.getNameSpace().isEmpty()) {
            this.nameSpace = this.config.getNameSpace();
        }
    }

    /**
     * <p>Adds the requested namespace to the logical operation, which is then passed into the REST
     * headers for said operation.</p>
     *
     * @param nameSpace The Vault namespace to access (e.g. <code>secret/</code>).
     * @return The Logical instance, with the namespace set.
     */
    public Logical withNameSpace(final String nameSpace) {
        this.nameSpace = nameSpace;

        return this;
    }

    /**
     * <p>Basic read operation to retrieve a secret.  A single secret key can map to multiple
     * name-value pairs, which can be retrieved from the response object.  E.g.:</p>
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
     * @return The response information returned from Vault
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code,
     * invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public LogicalResponse read(final String path) throws VaultException {
        if (this.engineVersionForSecretPath(path).equals(2)) {
            return read(path, logicalOperations.readV2);
        } else {
            return read(path, logicalOperations.readV1);
        }
    }

    private LogicalResponse read(final String path, final logicalOperations operation)
            throws VaultException {
        return retry(attempt -> {
            // Make an HTTP request to Vault
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/" + adjustPathForReadOrWrite(path,
                            config.getPrefixPathDepth(), operation))
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .get();

            // Validate response - don't treat 4xx class errors as exceptions, we want to return an error as the response
            if (restResponse.getStatus() != 200 && !(restResponse.getStatus() >= 400
                    && restResponse.getStatus() < 500)) {
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8),
                        restResponse.getStatus());
            }

            return new LogicalResponse(restResponse, attempt, operation);
        });
    }

    /**
     * <p>Basic read operation to retrieve a specified secret version for KV engine version 2. A
     * single secret key version can map to multiple name-value pairs, which can be retrieved from
     * the response object. E.g.:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final LogicalResponse response = vault.logical().read("secret/hello", true, 1);
     *
     * final String value = response.getData().get("value");
     * final String otherValue = response.getData().get("other_value");
     * }</pre>
     * </blockquote>
     *
     * @param path The Vault key value from which to read (e.g. <code>secret/hello</code>
     * @param shouldRetry Whether to try more than once
     * @param version The Integer version number of the secret to read, e.g. "1"
     * @return The response information returned from Vault
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code,
     * invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public LogicalResponse read(final String path, Boolean shouldRetry, final Integer version)
            throws VaultException {
        if (this.engineVersionForSecretPath(path) != 2) {
            throw new VaultException("Version reads are only supported in KV Engine version 2.");
        }

        return OperationsBase.retry(
                attempt -> {
                    // Make an HTTP request to Vault
                    final RestResponse restResponse =
                            new Rest() //NOPMD
                                    .url(config.getAddress() + "/v1/" + adjustPathForReadOrWrite(
                                            path,
                                            config.getPrefixPathDepth(), logicalOperations.readV2))
                                    .header("X-Vault-Token", config.getToken())
                                    .header("X-Vault-Namespace", this.nameSpace)
                                    .header("X-Vault-Request", "true")
                                    .parameter("version", version.toString())
                                    .connectTimeoutSeconds(config.getOpenTimeout())
                                    .readTimeoutSeconds(config.getReadTimeout())
                                    .sslVerification(config.getSslConfig().isVerify())
                                    .sslContext(config.getSslConfig().getSslContext())
                                    .get();

                    // Validate response - don't treat 4xx class errors as exceptions, we want to return an error as the response
                    if (restResponse.getStatus() != 200 && !(restResponse.getStatus() >= 400
                            && restResponse.getStatus() < 500)) {
                        throw new VaultException(
                                "Vault responded with HTTP status code: " + restResponse.getStatus()
                                        + "\nResponse body: " + new String(restResponse.getBody(),
                                        StandardCharsets.UTF_8),
                                restResponse.getStatus());
                    }

                    return new LogicalResponse(restResponse, attempt, logicalOperations.readV2);
                },
                shouldRetry ? config.getMaxRetries() : 1,
                config.getRetryIntervalMilliseconds()
        );
    }

    /**
     * <p>Basic operation to store secrets.  Multiple name value pairs can be stored under the same
     * secret key. E.g.:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final Map<String, String> nameValuePairs = new HashMap<String, Object>();
     * nameValuePairs.put("value", "foo");
     * nameValuePairs.put("other_value", "bar");
     *
     * final LogicalResponse response = vault.logical().write("secret/hello", nameValuePairs);
     * }</pre>
     * </blockquote>
     *
     * <p>The values in these name-value pairs may be booleans, numerics, strings, or nested JSON
     * objects.  However, be aware that this method does not recursively parse any nested
     * structures.  If you wish to write arbitrary JSON objects to Vault... then you should parse
     * them to JSON outside of this method, and pass them here as JSON strings.</p>
     *
     * @param path The Vault key value to which to write (e.g. <code>secret/hello</code>)
     * @param nameValuePairs Secret name and value pairs to store under this Vault key (can be
     * <code>null</code> for writing to keys that do not need or expect any fields to be specified)
     * @return The response information received from Vault
     * @throws VaultException If any errors occurs with the REST request, and the maximum number of
     * retries is exceeded.
     */
    public LogicalResponse write(final String path, final Map<String, Object> nameValuePairs)
            throws VaultException {
        if (engineVersionForSecretPath(path).equals(2)) {
            return write(path, nameValuePairs, logicalOperations.writeV2);
        } else {
            return write(path, nameValuePairs, logicalOperations.writeV1);
        }
    }

    private LogicalResponse write(final String path, final Map<String, Object> nameValuePairs,
            final logicalOperations operation) throws VaultException {

        return retry(attempt -> {
            JsonObject requestJson = Json.object();
            if (nameValuePairs != null) {
                for (final Map.Entry<String, Object> pair : nameValuePairs.entrySet()) {
                    final Object value = pair.getValue();
                    if (value == null) {
                        requestJson = requestJson.add(pair.getKey(), (String) null);
                    } else if (value instanceof Boolean) {
                        requestJson = requestJson.add(pair.getKey(), (Boolean) pair.getValue());
                    } else if (value instanceof Integer) {
                        requestJson = requestJson.add(pair.getKey(), (Integer) pair.getValue());
                    } else if (value instanceof Long) {
                        requestJson = requestJson.add(pair.getKey(), (Long) pair.getValue());
                    } else if (value instanceof Float) {
                        requestJson = requestJson.add(pair.getKey(), (Float) pair.getValue());
                    } else if (value instanceof Double) {
                        requestJson = requestJson.add(pair.getKey(), (Double) pair.getValue());
                    } else if (value instanceof JsonValue) {
                        requestJson = requestJson.add(pair.getKey(),
                                (JsonValue) pair.getValue());
                    } else {
                        requestJson = requestJson.add(pair.getKey(),
                                pair.getValue().toString());
                    }
                }
            }
            // Make an HTTP request to Vault
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/" + adjustPathForReadOrWrite(path,
                            config.getPrefixPathDepth(), operation))
                    .body(jsonObjectToWriteFromEngineVersion(operation, requestJson).toString()
                            .getBytes(StandardCharsets.UTF_8))
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .post();

            // HTTP Status should be either 200 (with content - e.g. PKI write) or 204 (no content)
            final int restStatus = restResponse.getStatus();
            if (restStatus == 200 || restStatus == 204 || (restResponse.getStatus() >= 400
                    && restResponse.getStatus() < 500)) {
                return new LogicalResponse(restResponse, attempt, operation);
            } else {
                throw new VaultException(
                        "Expecting HTTP status 204 or 200, but instead receiving " + restStatus
                                + "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8), restStatus);
            }
        });
    }

    /**
     * <p>Retrieve a list of keys corresponding to key/value pairs at a given Vault path.</p>
     *
     * <p>Key values ending with a trailing-slash characters are sub-paths.  Running a subsequent
     * <code>list()</code>
     * call, using the original path appended with this key, will retrieve all secret keys stored at
     * that sub-path.</p>
     *
     * <p>This method returns only the secret keys, not values.  To retrieve the actual stored
     * value for a key, use <code>read()</code> with the key appended onto the original base
     * path.</p>
     *
     * @param path The Vault key value at which to look for secrets (e.g. <code>secret</code>)
     * @return A list of keys corresponding to key/value pairs at a given Vault path, or an empty
     * list if there are none
     * @throws VaultException If any errors occur, or unexpected response received from Vault
     */
    public LogicalResponse list(final String path) throws VaultException {
        if (engineVersionForSecretPath(path).equals(2)) {
            return list(path, logicalOperations.listV2);
        } else {
            return list(path, logicalOperations.listV1);
        }
    }

    private LogicalResponse list(final String path, final logicalOperations operation)
            throws VaultException {
        LogicalResponse response = null;
        try {
            response = read(
                    adjustPathForList(path, config.getPrefixPathDepth(), operation),
                    operation
            );
        } catch (final VaultException e) {
            if (e.getHttpStatusCode() != 404) {
                throw e;
            }
        }

        return response;
    }

    /**
     * <p>Deletes the key/value pair located at the provided path.</p>
     *
     * <p>If the path represents a sub-path, then all of its contents must be deleted prior to
     * deleting the empty sub-path itself.</p>
     *
     * @param path The Vault key value to delete (e.g. <code>secret/hello</code>).
     * @return The response information received from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse delete(final String path) throws VaultException {
        if (engineVersionForSecretPath(path).equals(2)) {
            return delete(path, logicalOperations.deleteV2);
        } else {
            return delete(path, logicalOperations.deleteV1);
        }
    }

    private LogicalResponse delete(final String path, final Logical.logicalOperations operation)
            throws VaultException {
        return retry(attempt -> {
            // Make an HTTP request to Vault
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/" + adjustPathForDelete(path,
                            config.getPrefixPathDepth(), operation))
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .delete();

            // Validate response
            if (restResponse.getStatus() != 204) {
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8),
                        restResponse.getStatus());
            }

            return new LogicalResponse(restResponse, attempt, operation);
        });
    }

    /**
     * <p>Soft deletes the specified version of the key/value pair located at the provided
     * path.</p>
     * <p>
     * Only supported for KV Engine version 2. If the data is desired, it can be recovered with a
     * matching unDelete operation.
     *
     * <p>If the path represents a sub-path, then all of its contents must be deleted prior to
     * deleting the empty sub-path itself.</p>
     *
     * @param path The Vault key value to delete (e.g. <code>secret/hello</code>).
     * @param versions An array of Integers corresponding to the versions you wish to delete, e.g.
     * [1, 2] etc.
     * @return The response information received from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse delete(final String path, final int[] versions) throws VaultException {
        if (this.engineVersionForSecretPath(path) != 2) {
            throw new VaultException("Version deletes are only supported for KV Engine 2.");
        }
        intArrayCheck(versions);

        return retry(attempt -> {
            // Make an HTTP request to Vault
            JsonObject versionsToDelete = new JsonObject().add("versions", versions);
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/" + adjustPathForVersionDelete(path,
                            config.getPrefixPathDepth()))
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .body(versionsToDelete.toString().getBytes(StandardCharsets.UTF_8))
                    .post();

            // Validate response
            return getLogicalResponse(attempt, restResponse);
        });
    }

    private LogicalResponse getLogicalResponse(int retryCount, RestResponse restResponse)
            throws VaultException {
        if (restResponse.getStatus() != 204) {
            throw new VaultException(
                    "Vault responded with HTTP status code: " + restResponse.getStatus()
                            + "\nResponse body: " + new String(restResponse.getBody(),
                            StandardCharsets.UTF_8),
                    restResponse.getStatus());
        }
        return new LogicalResponse(restResponse, retryCount, logicalOperations.deleteV2);
    }

    private void intArrayCheck(int[] versions) {
        for (int i : versions) {
            if (i < 1) {
                throw new IllegalArgumentException("The document version must be 1 or greater.");
            }
        }
        Arrays.sort(versions);
    }

    /**
     * <p>Recovers a soft delete of the specified version of the key/value pair located at the
     * provided path.</p>
     * <p>
     * Only supported for KV Engine version 2.
     *
     * @param path The Vault key value to undelete (e.g. <code>secret/hello</code>).
     * @param versions An array of Integers corresponding to the versions you wish to undelete, e.g.
     * [1, 2] etc.
     * @return The response information received from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse unDelete(final String path, final int[] versions) throws VaultException {
        if (this.engineVersionForSecretPath(path) != 2) {
            throw new VaultException("Version undeletes are only supported for KV Engine 2.");
        }
        intArrayCheck(versions);

        return retry(attempt -> {
            // Make an HTTP request to Vault
            JsonObject versionsToUnDelete = new JsonObject().add("versions", versions);
            final RestResponse restResponse = new Rest() //NOPMD
                    .url(config.getAddress() + "/v1/" + adjustPathForVersionUnDelete(path,
                            config.getPrefixPathDepth()))
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .body(versionsToUnDelete.toString().getBytes(StandardCharsets.UTF_8))
                    .post();

            // Validate response
            if (restResponse.getStatus() != 204) {
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8),
                        restResponse.getStatus());
            }
            return new LogicalResponse(restResponse, attempt, logicalOperations.unDelete);
        });
    }

    /**
     * <p>Performs a hard delete of the specified version of the key/value pair located at the
     * provided path.</p>
     * <p>
     * Only supported for KV Engine version 2. There are no recovery options for the specified
     * version of the data deleted in this method.
     *
     * @param path The Vault key value to destroy (e.g. <code>secret/hello</code>).
     * @param versions An array of Integers corresponding to the versions you wish to destroy, e.g.
     * [1, 2] etc.
     * @return The response information received from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse destroy(final String path, final int[] versions) throws VaultException {
        if (this.engineVersionForSecretPath(path) != 2) {
            throw new VaultException("Secret destroys are only supported for KV Engine 2.");
        }
        intArrayCheck(versions);

        return retry(attempt -> {
            // Make an HTTP request to Vault
            JsonObject versionsToDestroy = new JsonObject().add("versions", versions);
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/" + adjustPathForVersionDestroy(path,
                            config.getPrefixPathDepth()))
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .body(versionsToDestroy.toString().getBytes(StandardCharsets.UTF_8))
                    .post();

            // Validate response
            return getLogicalResponse(attempt, restResponse);
        });
    }

    /**
     * <p>Performs an upgrade of the secrets engine version of the specified KV store to version
     * 2.</p>
     * <p>
     * There is no downgrading this operation back to version 1.
     *
     * @param kvPath The Vault kv engine to upgrade (e.g. <code>secret/</code>).
     * @return The response information received from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse upgrade(final String kvPath) throws VaultException {
        if (this.engineVersionForSecretPath(kvPath) == 2) {
            throw new VaultException("This KV engine is already version 2.");
        }

        return retry(attempt -> {
            // Make an HTTP request to Vault
            JsonObject kvToUpgrade = new JsonObject().add("options",
                    new JsonObject().add("version", 2));
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/sys/mounts/" + (kvPath.replaceAll("/", "")
                            + "/tune"))
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .body(kvToUpgrade.toString().getBytes(StandardCharsets.UTF_8))
                    .post();

            // Validate response
            if (restResponse.getStatus() != 200) {
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8),
                        restResponse.getStatus());
            }
            return new LogicalResponse(restResponse, attempt, logicalOperations.authentication);
        });
    }

    private Integer engineVersionForSecretPath(final String secretPath) {
        if (!this.config.getSecretsEnginePathMap().isEmpty()) {
            return this.config.getSecretsEnginePathMap().containsKey(secretPath + "/") ?
                    Integer.valueOf(this.config.getSecretsEnginePathMap().get(secretPath + "/"))
                    : this.config.getGlobalEngineVersion();
        }
        return this.config.getGlobalEngineVersion();
    }

    /**
     * <p>Provides the version of the secrets engine of the specified path, e.g. 1 or 2.</p>
     * First checks if the Vault config secrets engine path map contains the path. If not, then
     * defaults to the Global Engine version fallback.
     * <p>
     *
     * @param path The Vault secret path to check (e.g. <code>secret/</code>).
     * @return The response information received from Vault
     */
    public Integer getEngineVersionForSecretPath(final String path) {
        return this.engineVersionForSecretPath(path);
    }
}
