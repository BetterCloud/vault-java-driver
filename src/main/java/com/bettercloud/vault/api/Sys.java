package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;

/**
 * <p>This class has been deprecated, and will removed in a future release.  Please obtain a {@link Debug}
 * reference directly from the {@link com.bettercloud.vault.Vault} class instead:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final HealthResponse response = vault.debug().health();
 * }</pre>
 * </blockquote>
 *
 */
@Deprecated
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
    @Deprecated
    public Debug debug() {
        return new Debug(config);
    }

}
