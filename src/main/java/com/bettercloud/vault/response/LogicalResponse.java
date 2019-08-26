package com.bettercloud.vault.response;

import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is a container for the information returned by Vault in logical API
 * operations (e.g. read, write).
 */
public class LogicalResponse extends VaultResponse {

    private Map<String, String> data = new HashMap<>();
    private List<String> listData = new ArrayList<>();
    private JsonObject dataObject = null;
    private String leaseId;
    private Boolean renewable;
    private Long leaseDuration;

    /**
     * @param restResponse The raw HTTP response from Vault.
     * @param retries      The number of retry attempts that occurred during the API call (can be zero).
     * @param operation      The operation requested.
     */
    public LogicalResponse(final RestResponse restResponse, final int retries, final Logical.logicalOperations operation) {
        super(restResponse, retries);
        parseMetadataFields();
        parseResponseData(operation);
    }

    public Map<String, String> getData() {
        return data;
    }

    public List<String> getListData() {
        return listData;
    }

    public JsonObject getDataObject() {
        return dataObject;
    }

    public String getLeaseId() {
        return leaseId;
    }

    public Boolean getRenewable() {
        return renewable;
    }

    public Long getLeaseDuration() {
        return leaseDuration;
    }

    private void parseMetadataFields() {
        try {
            final String jsonString = new String(getRestResponse().getBody(), StandardCharsets.UTF_8);
            final JsonObject jsonObject = Json.parse(jsonString).asObject();

            this.leaseId = jsonObject.get("lease_id").asString();
            this.renewable = jsonObject.get("renewable").asBoolean();
            this.leaseDuration = jsonObject.get("lease_duration").asLong();
        } catch (Exception ignored) {
        }
    }

    private void parseResponseData(final Logical.logicalOperations operation) {
        try {
            final String jsonString = new String(getRestResponse().getBody(), StandardCharsets.UTF_8);
            JsonObject jsonObject = Json.parse(jsonString).asObject();
            if (operation.equals(Logical.logicalOperations.readV2)) {
                jsonObject = jsonObject.get("data").asObject();
            }
            data = new HashMap<>();
            dataObject = jsonObject.get("data").asObject();
            for (final JsonObject.Member member : dataObject) {
                final JsonValue jsonValue = member.getValue();
                if (jsonValue == null || jsonValue.isNull()) {
                    continue;
                } else if (jsonValue.isString()) {
                    data.put(member.getName(), jsonValue.asString());
                } else {
                    data.put(member.getName(), jsonValue.toString());
                }
            }
            // For list operations convert the array of keys to a list of values
            if (operation.equals(Logical.logicalOperations.listV1) || operation.equals(Logical.logicalOperations.listV2)) {
                if (
                        getRestResponse().getStatus() != 404
                                && data.get("keys") != null
                ) {

                    final JsonArray keys = Json.parse(data.get("keys")).asArray();
                    for (int index = 0; index < keys.size(); index++) {
                        listData.add(keys.get(index).asString());
                    }
                }

            }
        } catch (Exception ignored) {
        }
    }
}
