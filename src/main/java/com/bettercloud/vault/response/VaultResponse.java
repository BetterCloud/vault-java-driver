package com.bettercloud.vault.response;

import com.bettercloud.vault.rest.RestResponse;

public class VaultResponse {

    protected int retries;
    protected RestResponse restResponse;

    public int getRetries() {
        return retries;
    }

    public void setRetries(final int retries) {
        this.retries = retries;
    }

    public RestResponse getRestResponse() {
        return restResponse;
    }

    public void setRestResponse(RestResponse restResponse) {
        this.restResponse = restResponse;
    }

}
