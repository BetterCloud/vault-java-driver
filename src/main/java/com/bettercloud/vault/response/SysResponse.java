package com.bettercloud.vault.response;

import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.rest.RestResponse;

/**
 * This class is a container for the information returned by Vault in Sys API
 * operations (e.g. <code>v1/sys/revoke-prefix/aws</code>).
 */
public class SysResponse {

    private RestResponse restResponse;
    private int retries;

    /**
     * <p>Constructs a <code>SysResponse</code> object from the data received in a put
     * operation.</p>
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     */
    public SysResponse(final RestResponse restResponse, final int retries) throws VaultException {
        this.restResponse = restResponse;
        this.retries = retries;

        if (restResponse == null || restResponse.getBody() == null) {
            throw new VaultException("Response is null or contains a bad payload");
        }
    }

    public RestResponse getRestResponse() {
        return restResponse;
    }

    public int getRetries() {
        return retries;
    }

}
