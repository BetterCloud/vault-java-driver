package com.bettercloud.vault.response;

import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.rest.RestResponse;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * This class is a container for the information returned by Vault in <code>v1/sys/health</code>
 * operations.
 */
public class HealthResponse implements Serializable {

    private RestResponse restResponse;
    private int retries;

    private Boolean initialized;
    private Boolean sealed;
    private Boolean standby;
    private Long serverTimeUTC;

    /**
     * <p>Constructs a <code>HealthResponse</code> object from the data received in a health
     * check operation.</p>
     *
     * <p>Note that if the REST response is valid, but has an empty payload, this constructor
     * will silently return an instance with <code>initialized</code>, <code>sealed</code>,
     * <code>standby</code>, and <code>serverTimeUTC</code> set to <code>null</code>.  This
     * typically happens when you use optional parameters in the health call, to designate
     * non-standard HTTP status codes.  See docs for
     * {@link com.bettercloud.vault.api.Debug#health(Boolean, Integer, Integer, Integer)}.</p>
     *
     * @param restResponse The raw HTTP response from Vault
     * @param retries The number of retry attempts that occurred during the API call (can be zero)
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public HealthResponse(final RestResponse restResponse, final int retries) throws VaultException {
        this.restResponse = restResponse;
        this.retries = retries;

        if (restResponse == null) {
            throw new VaultException("Response is null");
        }
        if (restResponse.getBody() == null) {
            throw new VaultException("Response contains a bad payload", restResponse.getStatus());
        }
        if (restResponse.getBody().length > 0) {
            final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
            if (!mimeType.equals("application/json")) {
                throw new VaultException("Vault responded with MIME type: " + mimeType, restResponse.getStatus());
            }
            try {
                final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);//NOPMD
                final JsonObject jsonObject = Json.parse(jsonString).asObject();
                this.initialized = jsonObject.get("initialized") == null ? null : jsonObject.get("initialized").asBoolean();
                this.sealed = jsonObject.get("sealed") == null ? null : jsonObject.get("sealed").asBoolean();
                this.standby = jsonObject.get("standby") == null ? null : jsonObject.get("standby").asBoolean();
                this.serverTimeUTC = jsonObject.get("server_time_utc") == null ? null : jsonObject.get("server_time_utc").asLong();
            } catch(final Exception e) {
                throw new VaultException("Unable to parse JSON payload: " + e, restResponse.getStatus());
            }
        }
    }

    public RestResponse getRestResponse() {
        return restResponse;
    }

    public int getRetries() {
        return retries;
    }

    public Boolean getInitialized() {
        return initialized;
    }

    public Boolean getSealed() {
        return sealed;
    }

    public Boolean getStandby() {
        return standby;
    }

    /**
     * @return A value representing the number of milliseconds since the epoch.  With all of the changes in date API's between Java 8 and
     * pre-Java 8, it seemed best for the library not to convert this value into any particular one.
     */
    public Long getServerTimeUTC() {
        return serverTimeUTC;
    }

}
