package com.bettercloud.vault.api;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestException;
import com.bettercloud.vault.rest.RestResponse;

/**
 * <p>The implementing class for Vault's core/logical operations (e.g. read, write).</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for usage examples.</p>
 */
public class Logical {

    private final VaultConfig config;
    private static final int defaultSecretVersion = 0; //set as logical flag to read latest secrets of kv v1 and v2 without a given version number

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
     * @return The response information returned from Vault
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code, invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public LogicalResponse read(final String path) throws VaultException {
        return read(path, defaultSecretVersion);
    }

    /**
     * <p>Basic read operation to retrieve a secret of specific version</p>
     *
     * @param path          The Vault key value from which to read
     * @param secretVersion version number of key-value secret, set to 0 for kv version-1, set greater than 0 for kv version-2
     * @return The response information returned from Vault
     * @throws VaultException If any errors occurs with the REST request (e.g. non-200 status code, invalid JSON payload, etc), and the maximum number of retries is exceeded.
     */
    public LogicalResponse read(final String path, final int secretVersion) throws VaultException {
        final String version = getSecretEngineVersion(getPathSegments(path).get(0));
        if (secretVersion > 0 && "1".equals(version)) {
            throw new VaultException("Detected vault v1 which doesn't support kv v2 secret versioning, please enable KV Secrets Engine - Version 2 \n");
        }
        final String adjustedPath = adjustPathForReadOrWrite(path);
        final String url = getVersionedUrl(secretVersion, adjustedPath);
        int retryCount = 0;
        while (true) {
            try {
                // Make an HTTP request to Vault
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(url)
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .get();

                // Validate response
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }

                final Map<String, String> data = parseReadResponseData(restResponse, version);
                return new LogicalResponse(restResponse, retryCount, data);
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
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }

    private String getVersionedUrl(int secretVersion, String adjustedPath) {
        return secretVersion > 0 ? String.format("%s/v1/%s?version=%d", config.getAddress(), adjustedPath, secretVersion) :
                String.format("%s/v1/%s", config.getAddress(), adjustedPath);
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
     * @return The response information received from Vault
     * @throws VaultException If any errors occurs with the REST request, and the maximum number of retries is exceeded.
     */
    public LogicalResponse write(final String path, final Map<String, String> nameValuePairs) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                final String version = getSecretEngineVersion(getPathSegments(path).get(0));
                final String adjustedPath = adjustPathForReadOrWrite(path);

                JsonObject dataJson = Json.object();
                for (final Map.Entry<String, String> pair : nameValuePairs.entrySet()) {
                    dataJson = dataJson.add(pair.getKey(), pair.getValue());
                }
                JsonObject requestJson = Json.object();
                if ("2".equals(version)) {
                    // For version 2 secret backends, the payload goes inside of an additional nested "data" object
//                    requestJson = requestJson.add("data", Json.object().add("data", dataJson));
                    requestJson = requestJson.add("data", dataJson);
                } else {
                    requestJson = dataJson;
                }

                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/" + adjustedPath)
                        .body(requestJson.toString().getBytes("UTF-8"))
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .post();

                // HTTP Status should be either 200 (with content - e.g. PKI write) or 204 (no content)
                final int restStatus = restResponse.getStatus();
                if (restStatus == 204) {
                    return new LogicalResponse(restResponse, retryCount);
                } else if (restStatus == 200) {
                    final Map<String, String> data = parseWriteOrListResponseData(restResponse);
                    return new LogicalResponse(restResponse, retryCount, data);
                } else {
                    throw new VaultException("Expecting HTTP status 204 or 200, but instead receiving " + restStatus, restStatus);
                }
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
     * <p>Retrieve a list of keys corresponding to key/value pairs at a given Vault path.</p>
     *
     * <p>Key values ending with a trailing-slash characters are sub-paths.  Running a subsequent <code>list()</code>
     * call, using the original path appended with this key, will retrieve all secret keys stored at that sub-path.</p>
     *
     * <p>This method returns only the secret keys, not values.  To retrieve the actual stored value for a key,
     * use <code>read()</code> with the key appended onto the original base path.</p>
     *
     * @param path The Vault key value at which to look for secrets (e.g. <code>secret</code>)
     * @return A list of keys corresponding to key/value pairs at a given Vault path, or an empty list if there are none
     * @throws VaultException If any errors occur, or unexpected response received from Vault
     */
    public List<String> list(final String path) throws VaultException {

        final String adjustedPath = adjustPathForList(path);

        LogicalResponse response = null;
        int retryCount = 0;
        try {
            while (true) {
                try {
                    // Make an HTTP request to Vault
                    final RestResponse restResponse = new Rest()//NOPMD
                                                                .url(config.getAddress() + "/v1/" + adjustedPath)
                                                                .header("X-Vault-Token", config.getToken())
                                                                .connectTimeoutSeconds(config.getOpenTimeout())
                                                                .readTimeoutSeconds(config.getReadTimeout())
                                                                .sslPemUTF8(config.getSslPemUTF8())
                                                                .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                                                                .get();

                    // Validate response
                    if (restResponse.getStatus() != 200) {
                        throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                    }

                    final Map<String, String> data = parseWriteOrListResponseData(restResponse);
                    response = new LogicalResponse(restResponse, retryCount, data);
                    break;
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
                    } else if (e instanceof VaultException) {
                        // ... otherwise, give up.
                        throw (VaultException) e;
                    } else {
                        throw new VaultException(e);
                    }
                }
            }
        } catch (final VaultException e) {
            if (e.getHttpStatusCode() != 404) {
                throw e;
            }
        }

        final List<String> returnValues = new ArrayList<>();
        if (
                response != null
                && response.getRestResponse().getStatus() != 404
                && response.getData() != null
                && response.getData().get("keys") != null
        ) {

            final JsonArray keys = Json.parse(response.getData().get("keys")).asArray();
            for (int index = 0; index < keys.size(); index++) {
                returnValues.add(keys.get(index).asString());
            }
        }
        return returnValues;
    }

    /**
     * <p>Deletes the key/value pair located at the provided path.</p>
     *
     * <p>If the path represents a sub-path, then all of its contents must be deleted prior to deleting the empty
     * sub-path itself.</p>
     *
     * @param path The Vault key value to delete (e.g. <code>secret/hello</code>).
     * @return The response information received from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse delete(final String path) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                final String adjustedPath = adjustPathForDelete(path);

                // Make an HTTP request to Vault
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/" + adjustedPath)
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .delete();

                // Validate response
                if (restResponse.getStatus() != 204) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                return new LogicalResponse(restResponse, retryCount);
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
     * This logic will move into the <code>LogicalResponse constructor</code>.
     *
     * @param restResponse
     * @return
     * @throws VaultException
     */
    @Deprecated
    private Map<String, String> parseReadResponseData(final RestResponse restResponse, final String version) throws VaultException {
        final String jsonString = getJsonDataFromResponse(restResponse);
        JsonObject jsonData = Json.parse(jsonString).asObject().get("data").asObject();
        if ("2".equals(version)) {
            // A version 2 secret engine places the data within an additional nested "data" object
            jsonData = jsonData.get("data").asObject();
        }
        return buildResponseDataMap(jsonData);
    }

    /**
     * Convenience method to parse the payload of a write or list operation.  Unlike read operations, whose payloads vary depending on
     * whether the mount point uses a version 1 or 2 secret backend... writes and lists always use a version 1 style payload.
     *
     * @param restResponse
     * @return
     * @throws VaultException
     */
    private Map<String, String> parseWriteOrListResponseData(final RestResponse restResponse) throws VaultException {
        return parseReadResponseData(restResponse, "1");
    }

    /**
     * Performs basic validation, and extracts a JSON payload from a REST response body.
     *
     * @param restResponse
     * @return
     * @throws VaultException
     */
    private String getJsonDataFromResponse(final RestResponse restResponse) throws VaultException {
        final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
        if (!mimeType.equals("application/json")) {
            throw new VaultException("Vault responded with MIME type: " + mimeType);
        }
        try {
            return new String(restResponse.getBody(), "UTF-8");//NOPMD
        } catch (UnsupportedEncodingException e) {
            throw new VaultException(e);
        }
    }

    /**
     * Converts a parsed response payload into a Java map of name/value secret pairs.
     *
     * @param jsonData
     * @return
     */
    private Map<String, String> buildResponseDataMap(final JsonObject jsonData) {
        final Map<String, String> data = new HashMap<String, String>();//NOPMD
        for (final JsonObject.Member member : jsonData) {
            final JsonValue jsonValue = member.getValue();
            if (jsonValue == null || jsonValue.isNull()) {
                continue;
            } else if (jsonValue.isString()) {
                data.put(member.getName(), jsonValue.asString());
            } else {
                data.put(member.getName(), jsonValue.toString());
            }
        }
        return data;
    }

    /**
     * In version 1 style secret engines, the same path is used for all CRUD operations on a secret.  In version 2 though, the
     * path varies depending on the operation being performed.  When reading or writing a secret, you must inject the path
     * segment "data" right after the lowest-level path segment.
     *
     * @param path
     * @return
     */
    private String adjustPathForReadOrWrite(final String path) {
        final List<String> pathSegments = getPathSegments(path);
        final String version = getSecretEngineVersion(pathSegments.get(0));
        if ("2".equals(version)) {
            // Version 2
            final StringBuilder adjustedPath = new StringBuilder(addQualifierToPath(pathSegments, "data"));
            if (path.endsWith("/")) {
                adjustedPath.append("/");
            }
            return adjustedPath.toString();
        } else {
            // Version 1
            return path;
        }
    }

    /**
     * In version 1 style secret engines, the same path is used for all CRUD operations on a secret.  In version 2 though, the
     * path varies depending on the operation being performed.  When listing secrets available beneath a path, you must inject the
     * path segment "metadata" right after the lowest-level path segment.
     *
     * @param path
     * @return
     */
    private String adjustPathForList(final String path) {
        final List<String> pathSegments = getPathSegments(path);
        final String version = getSecretEngineVersion(pathSegments.get(0));
        final StringBuilder adjustedPath = new StringBuilder();
        if ("2".equals(version)) {
            // Version 2
            adjustedPath.append(addQualifierToPath(pathSegments, "metadata"));
            if (path.endsWith("/")) {
                adjustedPath.append("/");
            }
        } else {
            // Version 1
            adjustedPath.append(path);
        }
        adjustedPath.append("?list=true");
        return adjustedPath.toString();
    }

    /**
     * In version 1 style secret engines, the same path is used for all CRUD operations on a secret.  In version 2 though, the
     * path varies depending on the operation being performed.  When deleting secrets, you must inject the  path segment "metadata"
     * right after the lowest-level path segment.
     *
     * @param path
     * @return
     */
    private String adjustPathForDelete(final String path) {
        final List<String> pathSegments = getPathSegments(path);
        final String version = getSecretEngineVersion(pathSegments.get(0));
        if ("2".equals(version)) {
            final StringBuilder adjustedPath = new StringBuilder(addQualifierToPath(pathSegments, "metadata"));
            if (path.endsWith("/")) {
                adjustedPath.append("/");
            }
            return adjustedPath.toString();
        } else {
            return path;
        }
    }

    /**
     * Convenience method to split a Vault path into its path segments.
     *
     * @param path
     * @return
     */
    private List<String> getPathSegments(final String path) {
        final List<String> segments = new ArrayList<>();
        final StringTokenizer tokenizer = new StringTokenizer(path, "/");
        while (tokenizer.hasMoreTokens()) {
            segments.add(tokenizer.nextToken());
        }
        return segments;
    }

    /**
     * For a given mount point, looks up the corresponding secret engine version that was found during the "pre-flight check".
     * {@link Vault#collectSecretEngineVersions()}
     *
     * @param pathRoot
     * @return
     */
    private String getSecretEngineVersion(final String pathRoot) {
        return config.getSecretEngineVersions().containsKey(pathRoot + "/") ? config.getSecretEngineVersions().get(pathRoot + "/") : "null";
    }

    /**
     * Injects the supplied qualifier (either "data" or "metadata") into the second-from-the-root segment position, for a Vault
     * path to be converted for use with a version 2 secret engine.
     *
     * @param segments
     * @param qualifier
     * @return
     */
    private String addQualifierToPath(final List<String> segments, final String qualifier) {
        final StringBuilder adjustedPath = new StringBuilder(segments.get(0)).append('/').append(qualifier).append('/');
        for (int index = 1; index < segments.size(); index++) {
            adjustedPath.append(segments.get(index));
            if (index + 1 < segments.size()) {
                adjustedPath.append('/');
            }
        }
        return adjustedPath.toString();
    }

}
