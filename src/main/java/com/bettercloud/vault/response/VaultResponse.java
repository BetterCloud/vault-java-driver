package com.bettercloud.vault.response;

import com.bettercloud.vault.rest.RestResponse;

public class VaultResponse {

    protected RestResponse restResponse;
    protected int retries;

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
