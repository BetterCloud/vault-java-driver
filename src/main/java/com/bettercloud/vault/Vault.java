package com.bettercloud.vault;

import com.bettercloud.vault.api.Logical;

/**
 * <p>The Vault driver class, the primary interface through which dependent applications will
 * access Vault.</p>
 *
 * <p>This driver exposes a DSL, compartmentalizing the various endpoints of the HTTP API
 * (e.g. "/", "sys/init", "sys/seal") into separate implementation classes (e.g. <code>Logical</code>,
 * <code>Init</code>, etc).</p>
 *
 * <p>Example usage:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final VaultConfig config = new VaultConfig("http://127.0.0.1:8200", "eace6676-4d78-c687-4e54-03cad00e3abf");
 * final Vault vault = new Vault(config);
 * ...
 * vault.logical().write("secret/hello", "world");
 * ...
 * final String value = vault.logical().read("secret/hello");
 * }</pre>
 * </blockquote>
 */
public final class Vault {

    private final Logical logical;

    /**
     * Construct a Vault driver instance with the provided config settings.
     *
     * @param config Configuration settings for Vault interaction (e.g. server address, token, etc)
     */
    public Vault(final VaultConfig config) {
        this.logical = new Logical(config);
    }

    /**
     * Returns the implementing class for Vault's core/logical operations (e.g. read, write)
     *
     * @return The implementing class for Vault's core/logical operations (e.g. read, write)
     */
    public Logical logical() {
        return logical;
    }

}
