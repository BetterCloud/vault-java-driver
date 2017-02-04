package com.bettercloud.vault.response;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is a container for the information returned by Vault in logical API
 * operations (e.g. read, write).
 */
public class LogicalResponse extends VaultResponse {

    private Map<String, String> data = new HashMap<String, String>();
    private String leaseId;
    private Boolean renewable;
    private Long leaseDuration;

    /**
     * This constructor simply exposes the common base class constructor.
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     */
    public LogicalResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries);
        parseMetadataFields();
    }

    /**
     * This constructor also takes all of the values read by a Vault read operation.
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     * @param data All name/value pairs found in the <code>data</code> section of a Vault read operation response.
     */
    public LogicalResponse(
            final RestResponse restResponse,
            final int retries,
            final Map<String, String> data
    ) {
        super(restResponse, retries);
        this.data.putAll(data);
        parseMetadataFields();
    }

    public Map<String, String> getData() {
        return data;
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
            final String jsonString = new String(getRestResponse().getBody(), "UTF-8");
            final JsonObject jsonObject = Json.parse(jsonString).asObject();

            this.leaseId = jsonObject.get("lease_id").asString();
            this.renewable = jsonObject.get("renewable").asBoolean();
            this.leaseDuration = jsonObject.get("lease_duration").asLong();
        } catch (Exception e) {
            return;
        }
    }
}
