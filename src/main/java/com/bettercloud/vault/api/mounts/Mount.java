package com.bettercloud.vault.api.mounts;

import java.io.Serializable;

import lombok.Getter;

/**
 * <p>A container for options returned by mounts endpoints on the Secret Engine backend. This class is
 * meant for use with a builder pattern style.  Example usage:</p>
 * 
 * <blockquote>
 * <pre>{@code
 * final Mount options = new Mount()
 *                             .type(MountType.PKI)
 *                             .description("Some description about the secret engine");
 * }</pre>
 * </blockquote>
 */
public class Mount implements Serializable {
    private static final long serialVersionUID = 45748211702309181L;

    @Getter private MountType type;
    @Getter private String description;
    @Getter private MountConfig config;
    @Getter private Boolean local;
    @Getter private Boolean sealWrap;

    public Mount type(final MountType type) {
        this.type = type;
        return this;
    }

    public Mount description(final String description) {
        this.description = description;
        return this;
    }

    public Mount config(final MountConfig config) {
        this.config = config;
        return this;
    }

    public Mount local(final Boolean local) {
        this.local = local;
        return this;
    }

    public Mount sealWrap(final Boolean sealWrap) {
        this.sealWrap = sealWrap;
        return this;
    }
}
