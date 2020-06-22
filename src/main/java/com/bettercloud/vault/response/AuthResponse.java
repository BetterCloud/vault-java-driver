package com.bettercloud.vault.response;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.json.ParseException;
import com.bettercloud.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a container for the information returned by Vault in auth backend operations.
 */
public class AuthResponse extends VaultResponse {

    private Boolean renewable;
    private String authClientToken;
    private String tokenAccessor;
    private List<String> authPolicies;
    private long authLeaseDuration;
    private boolean authRenewable;
    private String appId;
    private String userId;
    private String username;
    private String nonce;

    /**
     * This constructor simply exposes the common base class constructor.
     *
     * @param restResponse The raw HTTP response from Vault.
     * @param retries      The number of retry attempts that occurred during the API call (can be zero).
     */
    public AuthResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries);

        try {
            final String responseJson = new String(restResponse.getBody(), StandardCharsets.UTF_8);
            final JsonObject jsonObject = Json.parse(responseJson).asObject();
            final JsonObject authJsonObject = jsonObject.get("auth").asObject();

            renewable = jsonObject.get("renewable").asBoolean();
            authLeaseDuration = authJsonObject.getInt("lease_duration", 0);
            authRenewable = authJsonObject.getBoolean("renewable", false);
            if (authJsonObject.get("metadata") != null && !authJsonObject.get("metadata").toString().equalsIgnoreCase("null")) {
                final JsonObject metadata = authJsonObject.get("metadata").asObject();
                appId = metadata.getString("app-id", "");
                userId = metadata.getString("user-id", "");
                username = metadata.getString("username", "");
                nonce = metadata.getString("nonce", "");
            }
            authClientToken = authJsonObject.getString("client_token", "");
            tokenAccessor = authJsonObject.getString("accessor", "");
            final JsonArray authPoliciesJsonArray = authJsonObject.get("policies").asArray();
            authPolicies = new ArrayList<>();
            for (final JsonValue authPolicy : authPoliciesJsonArray) {
                authPolicies.add(authPolicy.asString());
            }
        } catch (ParseException e) {
        }
    }

    public String getUsername() {
        return username;
    }

    public Boolean getRenewable() {
        return renewable;
    }

    public String getAuthClientToken() {
        return authClientToken;
    }

    public List<String> getAuthPolicies() {
        return authPolicies;
    }

    public long getAuthLeaseDuration() {
        return authLeaseDuration;
    }

    public boolean isAuthRenewable() {
        return authRenewable;
    }

    public String getAppId() {
        return appId;
    }

    public String getUserId() {
        return userId;
    }

    public String getNonce() { return nonce; }

    public String getTokenAccessor() { return tokenAccessor; }
}
