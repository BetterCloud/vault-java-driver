package com.bettercloud.vault.api.mounts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A container for options returned by mounts endpoints on the Secret Engine backend. This class is
 * meant for use with a builder pattern style.  Example usage:</p>
 *
 * <p>Most of the time this will be wrapped inside <code>Mount</code> object rather than directly sent to Vault backend
 * or back to the user.</p>
 *
 * <blockquote>
 * <pre>{@code
 * final MountConfig config = new MountConfig()
 *                                  .defaultLeaseTtl(2628000)
 *                                  .maxLeaseTtl(2628000)
 *                                  .description("description of pki");
 * }</pre>
 * </blockquote>
 */
public class MountConfig implements Serializable {
    private static final long serialVersionUID = 839595627039704093L;

    private Integer defaultLeaseTtl;
    private Integer maxLeaseTtl;
    private String description;
    private Boolean forceNoCache;
    private String pluginName;
    private List<String> auditNonHmacRequestKeys;
    private List<String> auditNonHmacResponseKeys;

    public MountConfig defaultLeaseTtl(final Integer defaultLeaseTtl) {
        this.defaultLeaseTtl = defaultLeaseTtl;
        return this;
    }

    public MountConfig maxLeaseTtl(final Integer maxLeaseTtl) {
        this.maxLeaseTtl = maxLeaseTtl;
        return this;
    }

    public MountConfig description(final String description) {
        this.description = description;
        return this;
    }

    public MountConfig forceNoCache(final Boolean forceNoCache) {
        this.forceNoCache = forceNoCache;
        return this;
    }

    public MountConfig pluginName(final String pluginName) {
        this.pluginName = pluginName;
        return this;
    }

    public MountConfig auditNonHmacRequestKeys(final List<String> auditNonHmacRequestKeys) {
        if (auditNonHmacRequestKeys != null) {
            this.auditNonHmacRequestKeys = new ArrayList<>();
            this.auditNonHmacRequestKeys.addAll(auditNonHmacRequestKeys);
        }
        return this;
    }

    public MountConfig auditNonHmacResponseKeys(final List<String> auditNonHmacResponseKeys) {
        if (auditNonHmacResponseKeys != null) {
            this.auditNonHmacResponseKeys = new ArrayList<>();
            this.auditNonHmacResponseKeys.addAll(auditNonHmacResponseKeys);
        }
        return this;
    }

    public List<String> getAuditNonHmacRequestKeys() {
        if (auditNonHmacRequestKeys == null) {
            return null;
        } else {
            return new ArrayList<>(auditNonHmacRequestKeys);
        }
    }

    public List<String> getAuditNonHmacResponseKeys() {
        if (auditNonHmacResponseKeys == null) {
            return null;
        } else {
            return new ArrayList<>(auditNonHmacResponseKeys);
        }
    }

    public Integer getDefaultLeaseTtl() {
        return defaultLeaseTtl;
    }

    public Integer getMaxLeaseTtl() {
        return maxLeaseTtl;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getForceNoCache() {
        return forceNoCache;
    }

    public String getPluginName() {
        return pluginName;
    }

}
