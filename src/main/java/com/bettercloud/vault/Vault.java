package com.bettercloud.vault;

import com.bettercloud.vault.api.Logical;

/**
 * The Vault driver class, the primary interface through which dependent applications will
 * access Vault.
 */
public final class Vault {

    private final Logical logical;

    public Vault(final VaultConfig config) {
        this.logical = new Logical(config);
    }

    public Logical logical() {
        return logical;
    }

}
