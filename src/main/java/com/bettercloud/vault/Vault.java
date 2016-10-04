package com.bettercloud.vault;

import com.bettercloud.vault.api.Auth;
import com.bettercloud.vault.api.Debug;
import com.bettercloud.vault.api.Leases;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.api.Sys;
import com.bettercloud.vault.api.pki.Pki;

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
 * final VaultConfig config = new VaultConfig("http://127.0.0.1:8200", "eace6676-4d78-c687-4e54-03cad00e3abf");
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

    /**
     * Construct a Vault driver instance with the provided config settings.
     *
     * @param vaultConfig Configuration settings for Vault interaction (e.g. server address, token, etc)
     */
    public Vault(final VaultConfig vaultConfig) {
        this.vaultConfig = vaultConfig;
    }

    /**
     * This method is chained ahead of endpoints (e.g. <code>logical()</code>, <code>auth()</code>,
     * etc... to specify retry rules for any API operations invoked on that endpoint.
     *
     * @param maxRetries The number of times that API operations will be retried when a failure occurs.
     * @param retryIntervalMilliseconds The number of milliseconds that the driver will wait in between retries.
     * @return
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
     * <p>This method has been deprecated, and will removed in a future release.  Please obtain a {@link Debug}
     * reference directly from the {@link #debug()} method instead:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final HealthResponse response = vault.debug().health();
     * }</pre>
     * </blockquote>
     *
     */
    @Deprecated
    public Sys sys() { return new Sys(vaultConfig); }
}
