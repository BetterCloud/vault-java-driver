package com.bettercloud.vault.response;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.json.ParseException;
import com.bettercloud.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a container for the information returned by Vault in lookup operations on auth backends.
 */
public class LookupResponse extends VaultResponse {

    private String accessor;
    private long creationTime;
    private long creationTTL;
    private String displayName;
    private long explicitMaxTTL;
    private String id;
    private Long lastRenewalTime;
    private int numUses;
    private boolean orphan;
    private String path;
    private List<String> policies;
    private boolean renewable;
    private long ttl;
    private String username;

    /**
     * This constructor simply exposes the common base class constructor.
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries      The number of retry attempts that occurred during the API call (can be zero).
     */
    public LookupResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries);

        try {
            final String responseJson = new String(restResponse.getBody(), StandardCharsets.UTF_8);
            final JsonObject jsonObject = Json.parse(responseJson).asObject();
            final JsonObject dataJsonObject = jsonObject.get("data").asObject();

            accessor = dataJsonObject.getString("accessor", "");
            creationTime = dataJsonObject.getLong("creation_time", 0);
            creationTTL = dataJsonObject.getLong("creation_ttl", 0);
            displayName = dataJsonObject.getString("display_name", "");
            explicitMaxTTL = dataJsonObject.getLong("explicit_max_ttl", 0);
            id = dataJsonObject.getString("id", "");
            final JsonValue lastRenewalTimeJsonValue = dataJsonObject.get("last_renewal_time");
            if (lastRenewalTimeJsonValue != null) {
                lastRenewalTime = lastRenewalTimeJsonValue.asLong();
            }
            if (dataJsonObject.get("metadata") != null && !dataJsonObject.get("metadata").toString().equalsIgnoreCase("null")) {
                final JsonObject metadata = dataJsonObject.get("metadata").asObject();
                username = metadata.getString("username", "");
            }
            numUses = dataJsonObject.getInt("num_uses", 0);
            orphan = dataJsonObject.getBoolean("orphan", true);
            path = dataJsonObject.getString("path", "");
            final JsonArray policiesJsonArray = dataJsonObject.get("policies").asArray();
            policies = new ArrayList<>();
            for (final JsonValue policy : policiesJsonArray) {
                policies.add(policy.asString());
            }
            renewable = dataJsonObject.getBoolean("renewable", false);
            ttl = dataJsonObject.getLong("ttl", 0);

        } catch (ParseException ignored) {
        }
    }

    public String getAccessor() {
        return accessor;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getCreationTTL() {
        return creationTTL;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getExplicitMaxTTL() {
        return explicitMaxTTL;
    }

    public String getId() {
        return id;
    }

    public Long getLastRenewalTime() {
        return lastRenewalTime;
    }

    public int getNumUses() {
        return numUses;
    }

    public boolean isOrphan() {
        return orphan;
    }

    public String getPath() {
        return path;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public long getTTL() {
        return ttl;
    }

    public String getUsername() {
        return username;
    }
}
