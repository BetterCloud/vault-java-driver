package com.bettercloud.vault.response;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.rest.RestResponse;

/**
 * <p><code>VaultResponse</code> is a common base class for the response objects returned by
 * all API methods.  It contains the bare minimum of information common to all Vault
 * responses (e.g. the raw HTTP response, the number of retry attempts if any).  API methods
 * which return additional information will use more specialized subclasses inheriting
 * from <code>VaultResponse</code>.</p>
 *
 * <p>NOTE: It turns out that not all API methods will populate <code>leaseId</code>,
 * <code>renewable</code>, and <code>leaseDuration</code>.  In fact, many response types won't.
 * So the next major release will implement those fields directly in the subclasses where they're
 * used.
 */
public class VaultResponse {

    private RestResponse restResponse;
    private int retries;

    @Deprecated
    private String leaseId;
    @Deprecated
    private Boolean renewable;
    @Deprecated
    private Long leaseDuration;

    /**
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     */
    public VaultResponse(final RestResponse restResponse, final int retries) {
        this.restResponse = restResponse;
        this.retries = retries;
        parseMetadataFields();
    }

    public RestResponse getRestResponse() {
        return restResponse;
    }

    @Deprecated
    public void setRestResponse(final RestResponse restResponse) {
        this.restResponse = restResponse;
        parseMetadataFields();
    }

    public int getRetries() {
        return retries;
    }

    @Deprecated
    public void setRetries(final int retries) {
        this.retries = retries;
    }

    public String getLeaseId() {
        return leaseId;
    }

    @Deprecated
    public void setLeaseId(final String leaseId) {
        this.leaseId = leaseId;
    }

    public Boolean getRenewable() {
        return renewable;
    }

    @Deprecated
    public void setRenewable(final Boolean renewable) {
        this.renewable = renewable;
    }

    @Deprecated
    public void baseSetRenewable(final Boolean renewable) {
        this.renewable = renewable;
    }

    public Long getLeaseDuration() {
        return leaseDuration;
    }

    @Deprecated
    public void setLeaseDuration(final Long leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    private void parseMetadataFields() {
        String jsonString;
        try {
            jsonString = new String(this.restResponse.getBody(), "UTF-8");
        } catch (Exception e) {
            return;
        }
        try {
            this.leaseId = Json.parse(jsonString).asObject().get("lease_id").asString();
        } catch (Exception e) {
        }
        try {
            this.renewable = Json.parse(jsonString).asObject().get("renewable").asBoolean();
        } catch (Exception e) {
        }
        try {
            this.leaseDuration = Json.parse(jsonString).asObject().get("lease_duration").asLong();
        } catch (Exception e) {
        }
    }
}
