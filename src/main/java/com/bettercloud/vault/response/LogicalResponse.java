package com.bettercloud.vault.response;

import com.bettercloud.vault.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is a container for the information returned by Vault in logical API
 * operations (e.g. read, write).
 */
public class LogicalResponse extends VaultResponse {

    private Map<String, String> data = new HashMap<String, String>();

    /**
     * This constructor simply exposes the common base class constructor.
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     */
    public LogicalResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries);
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
    }

    public Map<String, String> getData() {
        return data;
    }

    @Deprecated
    public void setData(final Map<String, String> data) {
        this.data = data;
    }
}
