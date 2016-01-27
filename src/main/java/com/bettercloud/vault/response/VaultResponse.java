package com.bettercloud.vault.response;

import com.bettercloud.vault.rest.RestResponse;

/**
 * <code>VaultResponse</code> is a common base class for the response objects returned by
 * all API methods.  It contains the bare minimum of information common to all Vault
 * responses (e.g. the raw HTTP response, the number of retry attempts if any).  API methods
 * which return additional information will use more specialized subclasses inheriting
 * from <code>VaultResponse</code>.
 */
public class VaultResponse {

    protected RestResponse restResponse;
    protected int retries;

    /**
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     */
    public VaultResponse(final RestResponse restResponse, final int retries) {
        this.restResponse = restResponse;
        this.retries = retries;
    }

    public RestResponse getRestResponse() {
        return restResponse;
    }

    public void setRestResponse(RestResponse restResponse) {
        this.restResponse = restResponse;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

}
