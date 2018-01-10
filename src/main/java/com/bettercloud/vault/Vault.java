package com.bettercloud.vault;

import com.bettercloud.vault.api.Auth;
import com.bettercloud.vault.api.Debug;
import com.bettercloud.vault.api.Leases;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.api.Seal;
import com.bettercloud.vault.api.pki.Pki;
import org.jetbrains.annotations.NotNull;

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

    private @NotNull final VaultConfig vaultConfig;

    /**
     * Construct a Vault driver instance with the provided config settings.
     *
     * @param vaultConfig Configuration settings for Vault interaction (e.g. server address, token, etc)
     */
    public Vault(@NotNull final VaultConfig vaultConfig) {
        this.vaultConfig = vaultConfig;
    }

    /**
     * This method is chained ahead of endpoints (e.g. <code>logical()</code>, <code>auth()</code>,
     * etc... to specify retry rules for any API operations invoked on that endpoint.
     *
     * @param maxRetries The number of times that API operations will be retried when a failure occurs
     * @param retryIntervalMilliseconds The number of milliseconds that the driver will wait in between retries
     * @return This object, with maxRetries and retryIntervalMilliseconds populated
     */
    public @NotNull Vault withRetries(final int maxRetries, final int retryIntervalMilliseconds) {
        this.vaultConfig.setMaxRetries(maxRetries);
        this.vaultConfig.setRetryIntervalMilliseconds(retryIntervalMilliseconds);
        return this;
    }

    /**
     * Returns the implementing class for Vault's core/logical operations (e.g. read, write).
     *
     * @return The implementing class for Vault's core/logical operations (e.g. read, write)
     */
    public @NotNull Logical logical() {
        return new Logical(vaultConfig);
    }

    /**
     * Returns the implementing class for operations on Vault's <code>/v1/auth/*</code> REST endpoints
     *
     * @return The implementing class for Vault's auth operations.
     */
    public @NotNull Auth auth() {
        return new Auth(vaultConfig);
    }

    /**
     * Returns the implementing class for Vault's PKI secret backend (i.e. <code>/v1/pki/*</code> REST endpoints).
     *
     * @return The implementing class for Vault's PKI secret backend.
     */
    public @NotNull Pki pki() {
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
    public @NotNull Pki pki(final String mountPath) {
        return new Pki(vaultConfig, mountPath);
    }

    /**
     * Returns the implementing class for Vault's lease operations (e.g. revoke, revoke-prefix).
     *
     * @return The implementing class for Vault's lease operations (e.g. revoke, revoke-prefix).
     */
    public @NotNull Leases leases() {
        return new Leases(vaultConfig);
    }

    /**
     * Returns the implementing class for Vault's debug operations (e.g. raw, health).
     *
     * @return The implementing class for Vault's debug operations (e.g. raw, health)
     */
    public @NotNull Debug debug() {
        return new Debug(vaultConfig);
    }

    /**
     * Returns the implementing class for Vault's seal operations (e.g. seal, unseal, sealStatus).
     *
     * @return The implementing class for Vault's seal operations (e.g. seal, unseal, sealStatus).
     */
    public @NotNull Seal seal() {
        return new Seal(vaultConfig);
    }
}
