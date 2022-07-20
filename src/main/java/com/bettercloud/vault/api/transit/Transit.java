package com.bettercloud.vault.api.transit;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.database.DatabaseStaticRoleOptions;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.TransitResponse;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * <p>The implementing class for operations on Vault's database backend.</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for usage examples.</p>
 */
public class Transit {

    private final VaultConfig config;
    private final String mountPath;
    private String nameSpace;

    public Transit withNameSpace(final String nameSpace) {
        this.nameSpace = nameSpace;
        return this;
    }

    /**
     * Constructor for use when the Transit backend is mounted on the default path (i.e. <code>/v1/transit</code>).
     *
     * @param config A container for the configuration settings needed to initialize a <code>Vault</code> driver instance
     */
    public Transit(final VaultConfig config) {
        this(config, "transit");
    }

    /**
     * Constructor for use when the Transit backend is mounted on some non-default custom path (e.g. <code>/v1/tr123</code>).
     *
     * @param config    A container for the configuration settings needed to initialize a <code>Vault</code> driver instance
     * @param mountPath The path on which your Vault Transit backend is mounted, without the <code>/v1/</code> prefix (e.g. <code>"root-ca"</code>)
     */
    public Transit(final VaultConfig config, final String mountPath) {
        this.config = config;
        this.mountPath = mountPath;
        if (this.config.getNameSpace() != null && !this.config.getNameSpace().isEmpty()) {
            this.nameSpace = this.config.getNameSpace();
        }
    }
    public TransitResponse createKey(String keyName)  throws VaultException {
        return createKey(keyName, null);
    }
    /**
     * <p>Operation to create an key using the Transit backend.  Relies on an authentication token being present in
     * the <code>VaultConfig</code> instance.</p>
     *
     * <p>This version of the method accepts a <code>KeyOptions</code> parameter, containing optional settings
     * for the key creation operation.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final KeyOptions options = new KeyOptions()
     *                              .type("aes128-gcm96")
     *                              .exportable(true);
     * final TransitResponse response = vault.transit().createKey("testKey", options);
     *
     * assertEquals(204, response.getRestResponse().getStatus());
     * }</pre>
     * </blockquote>
     *
     * @param keyName A name for the key to be created
     * @param options  Optional settings for the key to be created
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public TransitResponse createKey(String keyName, KeyOptions options)  throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                final String requestJson = keyOptionsToJson(options);

                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/keys/%s", config.getAddress(), this.mountPath, keyName))
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
                return new TransitResponse(restResponse, retryCount);
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
     * <p>Operation to retrieve an key using the Transit backend.  Relies on an authentication token being present in
     * the <code>VaultConfig</code> instance.</p>
     *
     * <p>The key information will be populated in the <code>keyOptions</code> field of the <code>TransitResponse</code>
     * return value.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     * final TransitResponse response = vault.pki().getRole("testRole");
     *
     * final KeyOptions details = response.getKeyOptions();
     * }</pre>
     * </blockquote>
     *
     * @param keyName The name of the key to retrieve
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public TransitResponse getKey(String keyName) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/keys/%s", config.getAddress(), this.mountPath, keyName))
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
                return new TransitResponse(restResponse, retryCount);
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
     * <p>Operation to delete an key using the Transit backend.  Relies on an authentication token being present in
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
     * final TransitResponse response = vault.transit().deleteKey("testKey");
     * assertEquals(204, response.getRestResponse().getStatus();
     * }</pre>
     * </blockquote>
     *
     * @param keyName The name of the key to delete
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public TransitResponse deleteKey(String keyName) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/keys/%s", config.getAddress(), this.mountPath, keyName))
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
                return new TransitResponse(restResponse, retryCount);
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
     * <p>Operation to encrypt data using the Transit Secret engine.
     * Relies on an authentication token being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>This version of the method accepts a <code>EncryptOptions</code> parameter, containing optional settings
     * for the encrypt data operation.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final TransitEncryptOptions options = new EncryptOptions()
     *                              .plaintext("test"getBytes());
     * final TransitResponse response = vault.transit().encryptData("encryptKey1", options);
     *
     * assertEquals(204, response.getRestResponse().getStatus());
     * }</pre>
     * </blockquote>
     *
     * @param keyName A name for the encrypt key to be used
     * @param options Data and params to encrypt data
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public TransitResponse encryptData(final String keyName, final EncryptOptions options) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final String requestJson = encryptOptionsToJson(options);

                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/encrypt/%s", config.getAddress(), this.mountPath, keyName))
                        .header("X-Vault-Token", config.getToken())
                        .header("X-Vault-Namespace", this.nameSpace)
                        .body(requestJson.getBytes(StandardCharsets.UTF_8))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .post();

                // Validate response
                if (restResponse.getStatus() != 200 && restResponse.getStatus() != 404) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                return new TransitResponse(restResponse, retryCount);
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
     * <p>Operation to decrypt data using the Transit Secret engine.
     * Relies on an authentication token being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>This version of the method accepts a <code>DecryptOptions</code> parameter, containing optional settings
     * for the encrypt data operation.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final TransitEncryptOptions options = new DecryptOptions()
     *                              .ciphertext("test"getBytes());
     * final TransitResponse response = vault.transit().decryptData("encryptKey1", options);
     *
     * assertEquals(204, response.getRestResponse().getStatus());
     * }</pre>
     * </blockquote>
     *
     * @param keyName A name for the encrypt key to be used
     * @param options Data and params to encrypt data
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public TransitResponse decryptData(final String keyName, final DecryptOptions options) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final String requestJson = decryptOptionsToJson(options);

                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/decrypt/%s", config.getAddress(), this.mountPath, keyName))
                        .header("X-Vault-Token", config.getToken())
                        .header("X-Vault-Namespace", this.nameSpace)
                        .body(requestJson.getBytes(StandardCharsets.UTF_8))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .post();

                // Validate response
                if (restResponse.getStatus() != 200 && restResponse.getStatus() != 404) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                return new TransitResponse(restResponse, retryCount);
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

    public TransitResponse dataKey(final String type, final String keyName) throws VaultException {
        return dataKey(type, keyName, null);
    }

    /**
     * <p>Operation to encrypt data using the Transit Secret engine.
     * Relies on an authentication token being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>This version of the method accepts a <code>EncryptOptions</code> parameter, containing optional settings
     * for the encrypt data operation.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final TransitEncryptOptions options = new EncryptOptions()
     *                              .plaintext("test"getBytes());
     * final TransitResponse response = vault.transit().encryptData("encryptKey1", options);
     *
     * assertEquals(204, response.getRestResponse().getStatus());
     * }</pre>
     * </blockquote>
     *
     * @param type A type for response (plaintext, wrapped)
     * @param keyName A name for the encrypt key to be used
     * @param options Data and params to encrypt data
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public TransitResponse dataKey(final String type, final String keyName, final DataKeyOptions options) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final String requestJson = dataKeyOptionsToJson(options);

                final RestResponse restResponse = new Rest()//NOPMD
                        .url(String.format("%s/v1/%s/datakey/%s/%s", config.getAddress(), this.mountPath, type, keyName))
                        .header("X-Vault-Token", config.getToken())
                        .header("X-Vault-Namespace", this.nameSpace)
                        .body(requestJson.getBytes(StandardCharsets.UTF_8))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .post();

                // Validate response
                if (restResponse.getStatus() != 200 && restResponse.getStatus() != 404) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus(), restResponse.getStatus());
                }
                return new TransitResponse(restResponse, retryCount);
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

    private String keyOptionsToJson(KeyOptions options){
        final JsonObject jsonObject = Json.object();

        if (options != null) {
            addJsonFieldIfNotNull(jsonObject, "convergent_encryption", options.getConvergentEncryption());
            addJsonFieldIfNotNull(jsonObject, "derived", options.getDerived());
            addJsonFieldIfNotNull(jsonObject, "exportable", options.getExportable());
            addJsonFieldIfNotNull(jsonObject, "allow_plaintext_backup", options.getAllowPlaintextBackup());
            addJsonFieldIfNotNull(jsonObject, "type", options.getType());
            addJsonFieldIfNotNull(jsonObject, "auto_rotate_period", options.getAutoRotatePeriod());
        }
        return jsonObject.toString();
    }

    private String encryptOptionsToJson(final EncryptOptions options) {
        final JsonObject jsonObject = Json.object();

        if (options != null) {
            addJsonFieldIfNotNull(jsonObject, "plaintext", Base64.getEncoder().encodeToString(options.getPlaintext()));
            addJsonFieldIfNotNull(jsonObject, "context", options.getContext());
            addJsonFieldIfNotNull(jsonObject, "key_version", options.getKeyVersion());
            addJsonFieldIfNotNull(jsonObject, "nonce", options.getNonce());
        }

        return jsonObject.toString();
    }

    private String decryptOptionsToJson(final DecryptOptions options) {
        final JsonObject jsonObject = Json.object();

        if (options != null) {
            addJsonFieldIfNotNull(jsonObject, "ciphertext", options.getCiphertext());
            addJsonFieldIfNotNull(jsonObject, "context", options.getContext());
            addJsonFieldIfNotNull(jsonObject, "nonce", options.getNonce());
        }

        return jsonObject.toString();
    }

    private String dataKeyOptionsToJson(final DataKeyOptions options) {
        final JsonObject jsonObject = Json.object();

        if (options != null) {
            addJsonFieldIfNotNull(jsonObject, "context", options.getContext());
            addJsonFieldIfNotNull(jsonObject, "nonce", options.getNonce());
            addJsonFieldIfNotNull(jsonObject, "bits", options.getBits());
        }

        return jsonObject.toString();
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
        } else if (value instanceof Integer) {
            jsonObject.add(name, (Integer) value);
        } else if (value instanceof byte[]){
            jsonObject.add(name, new String((byte[]) value));
        }

        return jsonObject;
    }



}
