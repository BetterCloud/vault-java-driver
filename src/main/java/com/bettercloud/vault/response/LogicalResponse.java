package com.bettercloud.vault.response;

import com.bettercloud.vault.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Document...
 */
public final class LogicalResponse extends VaultResponse {

    private Map<String, String> data = new HashMap<String, String>();

    public LogicalResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries);
    }

    public LogicalResponse(
            final RestResponse restResponse,
            final int retries,
            final Map<String, String> data
    ) {
        super(restResponse, retries);
        this.data.putAll(data);
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(final Map<String, String> data) {
        this.data = data;
    }
}
