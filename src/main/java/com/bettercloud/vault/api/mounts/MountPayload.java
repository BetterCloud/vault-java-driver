package com.bettercloud.vault.api.mounts;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A container for options sent to mounts endpoints on the Secret Engine backend as REST payload. This class is
 * meant for use with a builder pattern style.  Example usage:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final MountPayload payload = new MountPayload()
 *                                  .defaultLeaseTtl(TimeToLive.of(30, TimeUnit.MINUTES))
 *                                  .maxLeaseTtl(TimeToLive.of(30, TimeUnit.MINUTES))
 *                                  .description("description of pki");
 * }</pre>
 * </blockquote>
 */
public class MountPayload implements Serializable {
    private static final long serialVersionUID = 839595627039704093L;

    private TimeToLive defaultLeaseTtl;
    private TimeToLive maxLeaseTtl;
    private String description;
    private Boolean forceNoCache;
    private String pluginName;
    private Boolean local;
    private Boolean sealWrap;
    private List<String> auditNonHmacRequestKeys;
    private List<String> auditNonHmacResponseKeys;

    public MountPayload defaultLeaseTtl(final TimeToLive defaultLeaseTtl) {
        this.defaultLeaseTtl = defaultLeaseTtl;
        return this;
    }

    public MountPayload maxLeaseTtl(final TimeToLive maxLeaseTtl) {
        this.maxLeaseTtl = maxLeaseTtl;
        return this;
    }

    public MountPayload description(final String description) {
        this.description = description;
        return this;
    }

    public MountPayload forceNoCache(final Boolean forceNoCache) {
        this.forceNoCache = forceNoCache;
        return this;
    }

    public MountPayload pluginName(final String pluginName) {
        this.pluginName = pluginName;
        return this;
    }

    public MountPayload local(final Boolean local) {
        this.local = local;
        return this;
    }

    public MountPayload sealWrap(final Boolean sealWrap) {
        this.sealWrap = sealWrap;
        return this;
    }

    public MountPayload auditNonHmacRequestKeys(final List<String> auditNonHmacRequestKeys) {
        if (auditNonHmacRequestKeys != null) {
            this.auditNonHmacRequestKeys = new ArrayList<>();
            this.auditNonHmacRequestKeys.addAll(auditNonHmacRequestKeys);
        }
        return this;
    }

    public MountPayload auditNonHmacResponseKeys(final List<String> auditNonHmacResponseKeys) {
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

    public JsonObject toEnableJson(MountType type) {
        final JsonObject jsonObject = Json.object();

        jsonObject.addIfNotNull("type", type.value());
        jsonObject.addIfNotNull("description", this.description);
        jsonObject.addIfNotNull("config", this.toConfigJson());
        jsonObject.addIfNotNull("plugin_name", this.pluginName);
        jsonObject.addIfNotNull("local", this.local);
        jsonObject.addIfNotNull("seal_wrap", this.sealWrap);

        return jsonObject;
    }

    public JsonObject toTuneJson() {
        final JsonObject jsonObject = Json.object();

        if (this.defaultLeaseTtl != null) {
            jsonObject.addIfNotNull("default_lease_ttl", this.defaultLeaseTtl.toString());
        }

        if (this.maxLeaseTtl != null) {
            jsonObject.addIfNotNull("max_lease_ttl", this.maxLeaseTtl.toString());
        }

        jsonObject.addIfNotNull("description", this.description);

        if (this.auditNonHmacRequestKeys != null && this.auditNonHmacRequestKeys.size() > 0) {
            jsonObject.addIfNotNull("audit_non_hmac_request_keys", String.join(",", this.auditNonHmacRequestKeys));
        }

        if (this.auditNonHmacResponseKeys != null && this.auditNonHmacResponseKeys.size() > 0) {
            jsonObject.addIfNotNull("audit_non_hmac_response_keys", String.join(",", this.auditNonHmacResponseKeys));
        }

        return jsonObject;
    }

    private JsonObject toConfigJson() {
        final JsonObject jsonObject = toTuneJson();

        jsonObject.addIfNotNull("force_no_cache", this.forceNoCache);
        jsonObject.addIfNotNull("plugin_name", this.pluginName);

        if (jsonObject.isEmpty()) {
            return null;
        }

        return jsonObject;
    }

    public TimeToLive getDefaultLeaseTtl() {
        return defaultLeaseTtl;
    }

    public TimeToLive getMaxLeaseTtl() {
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

    public Boolean getLocal() {
        return local;
    }

    public Boolean getSealWrap() {
        return sealWrap;
    }

}
