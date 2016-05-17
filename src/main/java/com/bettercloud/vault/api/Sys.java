package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;

/**
 * <p>A wrapper around the implementing classes for all of Vault's various
 * <code>/v1/sys/*</code> endpoints.  Because there are so many of them (most Vault API operations
 * fall under that root path), this Java API groups them by the categories suggested on
 * the Vault documentation page (https://www.vaultproject.io/docs/http/index.html).</p>
 *
 * <p>To make calls on implementing classes within this wrapper, just go one level deeper
 * that usual in the builder pattern style:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final HealthResponse response = vault.sys().debug().health();
 * }</pre>
 * </blockquote>
 */
public class Sys {

    private final VaultConfig config;

    public Sys(final VaultConfig config) {
        this.config = config;
    }

    /**
     * Returns the implementing class for operations on Vault's <code>/v1/sys/*</code> REST endpoints,
     * under the "Debug" section of the Vault HTTP API docs (https://www.vaultproject.io/docs/http/index.html).
     *
     * @return The implementing class for debugging-related endpoints in the Vault HTTP API.
     */
    public Debug debug() {
        return new Debug(config);
    }

}
