package com.bettercloud.vault;

import com.bettercloud.vault.api.Auth;
import com.bettercloud.vault.api.Debug;
import com.bettercloud.vault.api.Leases;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.api.Seal;
import com.bettercloud.vault.api.database.Database;
import com.bettercloud.vault.api.mounts.Mounts;
import com.bettercloud.vault.api.pki.Pki;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestException;
import com.bettercloud.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>The Vault driver class, the primary interface through which dependent applications will access Vault.</p>
 *
 * <p>This driver exposes a DSL, compartmentalizing the various endpoints of the HTTP API (e.g. "/", "sys/init",
 * "sys/seal") into separate implementation classes (e.g. <code>Logical</code>, <code>Init</code>, etc).</p>
 *
 * <p>Example usage:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final VaultConfig config = new VaultConfig
 *                                    .address("http://127.0.0.1:8200")
 *                                    .token("eace6676-4d78-c687-4e54-03cad00e3abf")
 *                                    .build();
 * final Vault vault = new Vault(config);
 *
 * ...
 *
 * final Map<String, String> secrets = new HashMap<String, String>();
 * secrets.put("value", "world");
 * secrets.put("other_value", "You can store multiple name/value pairs under a given key");
 *
 * final LogicalResponse writeResponse = vault
 *                                         .withRetries(5, 1000)  // optional
 *                                         .logical()
 *                                         .write("secret/hello", secrets);
 *
 * ...
 *
 * final String value = vault.logical()
 *                        .read("secret/hello")
 *                        .getData().get("value");
 * }</pre>
 * </blockquote>
 */
public class Vault {

    private final VaultConfig vaultConfig;
    private Logger logger =  Logger.getLogger(Vault.class.getCanonicalName());

    /**
     * Construct a Vault driver instance with the provided config settings.
     *
     * @param vaultConfig Configuration settings for Vault interaction (e.g. server address, token, etc)
     *                    If the VaultConfig Engine version path map is not supplied in the config, default to global KV
     *                    engine version 2.
     */
    public Vault(final VaultConfig vaultConfig) {
        this.vaultConfig = vaultConfig;
        if (this.vaultConfig.getNameSpace() != null && !this.vaultConfig.getNameSpace().isEmpty()) {
            logger.info(String.format("The NameSpace %s has been bound to this Vault instance. Please keep this in mind when running operations.", this.vaultConfig.getNameSpace()));
        }
        if (this.vaultConfig.getSecretsEnginePathMap().isEmpty() && this.vaultConfig.getGlobalEngineVersion() == null) {
            logger.info("Constructing a Vault instance with no provided Engine version, defaulting to version 2.");
            this.vaultConfig.setEngineVersion(2);
        }
    }

    /**
     * Construct a Vault driver instance with the provided config settings, and use the provided global KV Engine version for all secrets.
     *
     * @param vaultConfig             Configuration settings for Vault interaction (e.g. server address, token, etc)
     * @param engineVersion           Which version of the Key/Value Secret Engine to use globally (i.e. 1 or 2)
     */
    public Vault(final VaultConfig vaultConfig, final Integer engineVersion) {
        if (engineVersion < 1 || engineVersion > 2) {
            throw new IllegalArgumentException("The Engine version must be '1' or '2', the version supplied was: '"
                    + engineVersion + "'.");
        }
        vaultConfig.setEngineVersion(engineVersion);
        this.vaultConfig = vaultConfig;
        if (this.vaultConfig.getNameSpace() != null && !this.vaultConfig.getNameSpace().isEmpty()) {
            logger.info(String.format("The Namespace %s has been bound to this Vault instance. Please keep this in mind when running operations.", this.vaultConfig.getNameSpace()));
        }
    }

    /**
     * Construct a Vault driver instance with the provided config settings.
     *
     * @param vaultConfig             Configuration settings for Vault interaction (e.g. server address, token, etc)
     *                                If the Secrets engine version path map is not provided, or does not contain the
     *                                requested secret, fall back to the global version supplied.
     * @param useSecretsEnginePathMap Whether to use a provided KV Engine version map from the Vault config, or generate one.
     *                                If a secrets KV Engine version map is not supplied, use Vault APIs to determine the
     *                                KV Engine version for each secret. This call requires admin rights.
     * @param globalFallbackVersion   The Integer version of the KV Engine to use as a global fallback.
     *
     * @throws VaultException         If any error occurs
     */
    public Vault(final VaultConfig vaultConfig, final Boolean useSecretsEnginePathMap, final Integer globalFallbackVersion)
            throws VaultException {
        this.vaultConfig = vaultConfig;
        if (this.vaultConfig.getNameSpace() != null && !this.vaultConfig.getNameSpace().isEmpty()) {
            logger.info(String.format("The Namespace %s has been bound to this Vault instance. Please keep this in mind when running operations.", this.vaultConfig.getNameSpace()));
        }
        this.vaultConfig.setEngineVersion(globalFallbackVersion);
        if (useSecretsEnginePathMap && this.vaultConfig.getSecretsEnginePathMap().isEmpty()) {
            try {
                logger.info("No secrets Engine version map was supplied, attempting to generate one.");
                final Map<String, String> secretsEnginePathMap = collectSecretEngineVersions();
                assert secretsEnginePathMap != null;
                this.vaultConfig.getSecretsEnginePathMap().putAll(secretsEnginePathMap);
            } catch (Exception e) {
                throw new VaultException(String.format("An Engine KV version map was not supplied, and unable to determine " +
                        "KV Engine " +
                        "version, " + "due to exception: %s", e.getMessage() + ". Do you have admin rights?"));
            }
        }
    }

    /**
     * This method is chained ahead of endpoints (e.g. <code>logical()</code>, <code>auth()</code>,
     * etc... to specify retry rules for any API operations invoked on that endpoint.
     *
     * @param maxRetries                The number of times that API operations will be retried when a failure occurs
     * @param retryIntervalMilliseconds The number of milliseconds that the driver will wait in between retries
     * @return This object, with maxRetries and retryIntervalMilliseconds populated
     */
    public Vault withRetries(final int maxRetries, final int retryIntervalMilliseconds) {
        this.vaultConfig.setMaxRetries(maxRetries);
        this.vaultConfig.setRetryIntervalMilliseconds(retryIntervalMilliseconds);
        return this;
    }

    /**
     * Returns the implementing class for Vault's core/logical operations (e.g. read, write).
     *
     * @return The implementing class for Vault's core/logical operations (e.g. read, write)
     */
    public Logical logical() {
        return new Logical(vaultConfig);
    }

    /**
     * Returns the implementing class for operations on Vault's <code>/v1/auth/*</code> REST endpoints
     *
     * @return The implementing class for Vault's auth operations.
     */
    public Auth auth() {
        return new Auth(vaultConfig);
    }

    /**
     * Returns the implementing class for Vault's PKI secret backend (i.e. <code>/v1/pki/*</code> REST endpoints).
     *
     * @return The implementing class for Vault's PKI secret backend.
     */
    public Pki pki() {
        return new Pki(vaultConfig);
    }

    /**
     * <p>Returns the implementing class for Vault's PKI secret backend, using a custom path when that backend is
     * mounted on something other than the default (i.e. <code>/v1/pki</code>).</p>
     *
     * <p>For instance, if your PKI backend is instead mounted on <code>/v1/root-ca</code>, then <code>"root-ca"</code>
     * would be passed via the <code>mountPath</code> parameter.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig().address(...).token(...).build();
     * final Vault vault = new Vault(config);
     * final PkiResponse response = vault.pki("root-ca").createOrUpdateRole("testRole");
     *
     * assertEquals(204, response.getRestResponse().getStatus());
     * }</pre>
     * </blockquote>
     *
     * @param mountPath The path on which your Vault PKI backend is mounted, without the <code>/v1/</code> prefix
     * @return The implementing class for Vault's PKI secret backend.
     */
    public Pki pki(final String mountPath) {
        return new Pki(vaultConfig, mountPath);
    }

    public Database database() { return new Database(vaultConfig); }

    public Database database(final String mountPath) { return new Database(vaultConfig, mountPath); }

    /**
     * Returns the implementing class for Vault's lease operations (e.g. revoke, revoke-prefix).
     *
     * @return The implementing class for Vault's lease operations (e.g. revoke, revoke-prefix).
     */
    public Leases leases() {
        return new Leases(vaultConfig);
    }

    /**
     * Returns the implementing class for Vault's debug operations (e.g. raw, health).
     *
     * @return The implementing class for Vault's debug operations (e.g. raw, health)
     */
    public Debug debug() {
        return new Debug(vaultConfig);
    }

    /**
     * Returns the implementing class for Vault's sys mounts operations (i.e. <code>/v1/sys/mounts/*</code> REST endpoints).
     *
     * @return the implementing class for Vault's sys mounts operations
     */
    public Mounts mounts() {
        return new Mounts(vaultConfig);
    }

    /**
     * Returns the implementing class for Vault's seal operations (e.g. seal, unseal, sealStatus).
     *
     * @return The implementing class for Vault's seal operations (e.g. seal, unseal, sealStatus).
     */
    public Seal seal() {
        return new Seal(vaultConfig);
    }

    /**
     * Makes a REST call to Vault, to collect information on which secret engine version (if any) is used by each available
     * mount point.  Possibilities are:
     *
     * <ul>
     * <li>"2" - A mount point running on Vault 0.10 or higher, configured to use the engine 2 API</li>
     * <li>"1" - A mount point running on Vault 0.10 or higher, configured to use the engine 1 API</li>
     * <li>"unknown" - A mount point running on an older version of Vault.  Can more or less be treated as "1".</li>
     * </ul>
     * <p>
     * IMPORTANT:  Whichever authentication mechanism is being used with the <code>VaultConfig</code> object, that principal
     * needs permission to access the <code>/v1/sys/mounts</code> REST endpoint.
     *
     * @return A map of mount points (e.g. "/secret") to secret engine version numbers (e.g. "2")
     */
    private Map<String, String> collectSecretEngineVersions() {
        try {
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(vaultConfig.getAddress() + "/v1/sys/mounts")
                    .header("X-Vault-Token", vaultConfig.getToken())
                    .header("X-Vault-Namespace", this.vaultConfig.getNameSpace())
                    .connectTimeoutSeconds(vaultConfig.getOpenTimeout())
                    .readTimeoutSeconds(vaultConfig.getReadTimeout())
                    .sslVerification(vaultConfig.getSslConfig().isVerify())
                    .sslContext(vaultConfig.getSslConfig().getSslContext())
                    .get();
            if (restResponse.getStatus() != 200) {
                return null;
            }

            final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
            final Map<String, String> data = new HashMap<>();
            final JsonObject jsonData = Json.parse(jsonString).asObject().get("data").asObject();
            for (JsonObject.Member member : jsonData) {
                final String name = member.getName();
                String version = "unknown";

                final JsonValue options = member.getValue().asObject().get("options");
                if (options != null && options.isObject()) {
                    final JsonValue ver = options.asObject().get("version");
                    if (ver != null && ver.isString()) {
                        version = ver.asString();
                    }
                }
                data.put(name, version);
            }
            return data;
        } catch (RestException e) {
            System.err.print(String.format("Unable to retrieve the KV Engine secrets, due to exception: %s", e.getMessage()));
            return null;
        }
    }

    public Map<String, String> getSecretEngineVersions() {
        return this.collectSecretEngineVersions();
    }
}
