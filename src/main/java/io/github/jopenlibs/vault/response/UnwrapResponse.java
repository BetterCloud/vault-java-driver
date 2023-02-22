package io.github.jopenlibs.vault.response;

import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.rest.RestResponse;

/**
 * This class is a container for the information returned by Vault in unwrap backend operations.
 */
public class UnwrapResponse extends AuthResponse {

    /**
     * This constructor simply exposes the common base class constructor.
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     */
    public UnwrapResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries);
    }

    public JsonObject getData() {
        assert jsonResponse.get("data").isObject();

        return jsonResponse.get("data").asObject();
    }
}
