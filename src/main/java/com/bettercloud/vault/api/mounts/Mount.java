package com.bettercloud.vault.api.mounts;

import java.io.Serializable;

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

    private MountType type;
    private String description;
    private MountConfig config;
    private Boolean local;
    private Boolean sealWrap;

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

    public MountType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public MountConfig getConfig() {
        return config;
    }

    public Boolean getLocal() {
        return local;
    }

    public Boolean getSealWrap() {
        return sealWrap;
    }

}
