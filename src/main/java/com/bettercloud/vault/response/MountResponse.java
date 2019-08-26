package com.bettercloud.vault.response;

import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.api.mounts.Mount;
import com.bettercloud.vault.api.mounts.MountConfig;
import com.bettercloud.vault.api.mounts.MountType;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonObject.Member;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.rest.RestResponse;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class is a container for the information returned by Vault in /sys/mounts/ API
 * operations (e.g. enable/disable secret engine mountpoint, read/tune mountpoint configurations)
 */
public class MountResponse extends LogicalResponse {
    private static final long serialVersionUID = -7066405243425032451L;

    private final Mount mount;
    private final Map<String, Mount> mounts;

    /**
     * @param restResponse The raw response received from Vault
     * @param retries The number of retries that were performed for this operation
     * @param isList the flag to indicate if we're expecting a list of Mounts from Vault or not
     */
    public MountResponse(final RestResponse restResponse, final int retries, final boolean isList) {
        super(restResponse, retries, Logical.logicalOperations.mount);

        mount = isList ? null : buildMount(this.getDataObject());
        mounts = isList ? buildMountsMap() : null;
    }

    public Mount getMount() {
        return mount;
    }

    public Map<String, Mount> getMounts() {
        return mounts;
    }

    private Mount buildMount(JsonObject data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        final MountType type = MountType.of(data.getString("type"));
        final String description = data.getString("description");
        final MountConfig config = buildMountConfig(data);
        final Boolean local = data.getBoolean("local");
        final Boolean sealWrap = data.getBoolean("seal_wrap");

        if (type == null && description == null && config == null && local == null && sealWrap == null) {
            return null;
        }

        return new Mount()
                .type(type)
                .description(description)
                .config(config)
                .local(local)
                .sealWrap(sealWrap);
    }

    private MountConfig buildMountConfig(JsonObject data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        JsonValue object = data.get("config");
        JsonObject config = object != null ? object.asObject() : data;

        if (config == null || config.isEmpty()) {
            return null;
        }

        final Integer defaultLeaseTtl = config.getInt("default_lease_ttl");
        final Integer maxLeaseTtl = config.getInt("max_lease_ttl");
        final String description = config.getString("description");
        final Boolean forceNoCache = config.getBoolean("force_no_cache");
        final String pluginName = config.getString("plugin_name");

        if (defaultLeaseTtl == null && maxLeaseTtl == null && description == null && forceNoCache == null && pluginName == null) {
            return null;
        }

        return new MountConfig()
                .defaultLeaseTtl(defaultLeaseTtl)
                .maxLeaseTtl(maxLeaseTtl)
                .description(description)
                .forceNoCache(forceNoCache)
                .pluginName(pluginName);
    }

    private Map<String, Mount> buildMountsMap() {
        final JsonObject data = this.getDataObject();

        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(data.spliterator(), false)
                .collect(Collectors.toMap(Member::getName, member -> buildMount(member.getValue().asObject())));
    }
}
