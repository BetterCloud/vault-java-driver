package io.github.jopenlibs.vault.response;

import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.json.JsonValue;
import io.github.jopenlibs.vault.json.ParseException;
import io.github.jopenlibs.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;

/**
 * When a response is wrapped, the normal API response from Vault does not contain the original secret,
 * but rather contains a set of information related to the response-wrapping token.
 */
public class WrapResponse extends VaultResponse {
    private Boolean renewable;
    private String token;
    private String accessor;
    private int ttl;
    private String creationTime;
    private String creationPath;

    /**
     * Parse response-wrapping and create an instance of response.
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries      The number of retry attempts that occurred during the API call (can be zero).
     */
    public WrapResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries);

        try {
            final String responseJson = new String(restResponse.getBody(), StandardCharsets.UTF_8);
            JsonObject jsonResponse = Json.parse(responseJson).asObject();
            JsonValue wrapInfoJsonVal = jsonResponse.get("wrap_info");
            if (wrapInfoJsonVal != null && !wrapInfoJsonVal.isNull()) {
                final JsonObject wrapInfoJsonObject = wrapInfoJsonVal.asObject();
                token = wrapInfoJsonObject.getString("token", null);
                accessor = wrapInfoJsonObject.getString("accessor", null);
                ttl = wrapInfoJsonObject.getInt("ttl", 0);
                creationTime = wrapInfoJsonObject.getString("creation_time", null);
                creationPath = wrapInfoJsonObject.getString("creation_path", null);
            }

            renewable = jsonResponse.get("renewable").asBoolean();
        } catch (ParseException e) {
            // No-op.
        }
    }

    public Boolean getRenewable() {
        return renewable;
    }

    /**
     * Get response-wrapped token.
     *
     * @return response-wrapped token.
     */
    public String getToken() {
        return token;
    }

    /**
     *  If the wrapped response is an authentication response containing a Vault token,
     *  this is the value of the wrapped token's accessor. This is useful for orchestration
     *  systems (such as Nomad) to be able to control the lifetime of secrets based on
     *  their knowledge of the lifetime of jobs, without having to actually unwrap
     *  the response-wrapping token or gain knowledge of the token ID inside
     *
     * @return Wrapped Accessor.
     */
    public String getAccessor() {
        return accessor;
    }

    /**
     * Get wrapped token TTL.
     *
     * @return Wrapped token TTL.
     */
    public int getTtl() {
        return ttl;
    }

    /**
     * Get the time that the response-wrapping token was created
     *
     * @return The time that the response-wrapping token was created;
     */
    public String getCreationTime() {
        return creationTime;
    }

    /**
     * Get the API path that was called in the original request
     *
     * @return The API path that was called in the original request
     */
    public String getCreationPath() {
        return creationPath;
    }
}
