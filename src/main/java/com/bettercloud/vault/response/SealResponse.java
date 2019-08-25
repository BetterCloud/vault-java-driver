package com.bettercloud.vault.response;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.ParseException;
import com.bettercloud.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;

/**
 * This class is a container for the information returned by Vault in <code>v1/sys/*seal*</code>
 * operations.
 */
public class SealResponse extends VaultResponse {
    private Boolean sealed;
    private Long threshold;
    private Long numberOfShares;
    private Long progress;

    /**
     * This constructor simply exposes the common base class constructor.
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries      The number of retry attempts that occurred during the API call (can be zero).
     */
    public SealResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries);

        try {
            final String responseJson = new String(restResponse.getBody(), StandardCharsets.UTF_8);
            final JsonObject jsonObject = Json.parse(responseJson).asObject();

            sealed = jsonObject.getBoolean("sealed", false);
            threshold = jsonObject.getLong("t", 0);
            numberOfShares = jsonObject.getLong("n", 0);
            progress = jsonObject.getLong("progress", 0);

        } catch (ParseException ignored) {
        }
    }

    public Boolean getSealed() {
        return sealed;
    }

    public Long getThreshold() {
        return threshold;
    }

    public Long getNumberOfShares() {
        return numberOfShares;
    }

    public Long getProgress() {
        return progress;
    }
}
