package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.rest.Response;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestException;

import java.io.UnsupportedEncodingException;

public class Auth {

    private final VaultConfig config;

    public Auth(final VaultConfig config) {
        this.config = config;
    }

    /**
     * Basic login operation to authenticate to an app-id backend.
     *
     * TODO: Perhaps devise a complex return type, capturing the value read along with all metadata, rather than just returning the value as a plain string.
     *
     * @param path The path on which the authentication is performed (e.g. <code>auth/app-id/login</code>)
     * @param appId The app-id used for authentication
     * @param userId The user-id used for authentication
     * @return The auth token
     * @throws VaultException
     */
    public AuthResponse loginByAppID(final String path, final String appId, final String userId) throws VaultException {
        try {
            // HTTP request to Vault
            final Response response = new Rest()
                    .url(config.getAddress() + "/v1/auth/" + path)
                    .body(Json.object().add("app_id", appId).add("user_id",userId).toString().getBytes("UTF-8"))
                    .post();

            // Validate response
            if (response.getStatus() != 200) {
                throw new VaultException("Vault responded with HTTP status code: " + response.getStatus());
            }
            final String mimeType = response.getMimeType() == null ? "null" : response.getMimeType();
            if (!mimeType.equals("application/json")) {
                throw new VaultException("Vault responded with MIME type: " + mimeType);
            }
            String jsonString;
            try {
                jsonString = new String(response.getBody(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new VaultException(e);
            }
            return buildAuthResponse(jsonString);
        } catch (RestException e) {
            throw new VaultException(e);
        } catch (UnsupportedEncodingException e) {
            throw new VaultException(e);
        }
    }

    /**
     * Basic login operation to authenticate to an username & password backend.
     *
     * TODO: Perhaps devise a complex return type, capturing the value read along with all metadata, rather than just returning the value as a plain string.
     *
     * @param path The path on which the authentication is performed (e.g. <code>auth/userpass/login/username</code>)
     * @param password The password used for authentication
     * @return The auth token
     * @throws VaultException
     */
    public AuthResponse loginByUsernamePassword(final String path, final String password) throws VaultException {
        try {
            // HTTP request to Vault
            final Response response = new Rest()
                    .url(config.getAddress() + "/v1/auth/" + path)
                    .body(Json.object().add("password", password).toString().getBytes("UTF-8"))
                    .post();

            // Validate response
            if (response.getStatus() != 200) {
                throw new VaultException("Vault responded with HTTP status code: " + response.getStatus());
            }
            final String mimeType = response.getMimeType() == null ? "null" : response.getMimeType();
            if (!mimeType.equals("application/json")) {
                throw new VaultException("Vault responded with MIME type: " + mimeType);
            }
            String jsonString;
            try {
                jsonString = new String(response.getBody(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new VaultException(e);
            }
            return buildAuthResponse(jsonString);
        } catch (RestException e) {
            throw new VaultException(e);
        } catch (UnsupportedEncodingException e) {
            throw new VaultException(e);
        }
    }

    private AuthResponse buildAuthResponse(final String responseJson){
        final AuthResponse authResponse = new AuthResponse();
        final JsonObject jsonObject = Json.parse(responseJson).asObject();
        authResponse.setLease_id(jsonObject.getString("lease_id",""));
        authResponse.setRenewable(jsonObject.getBoolean("renewable",false));
        authResponse.setLease_duration(jsonObject.getInt("lease_duration",0));
        final JsonObject authJsonObject = jsonObject.get("auth").asObject();
        authResponse.setAuth_client_token(authJsonObject.getString("client_token",""));
        final JsonArray authPoliciesJsonArray = authJsonObject.get("policies").asArray();
        String[] authPolicies = new String[authPoliciesJsonArray.size()];
        for( int i=0; i < authPoliciesJsonArray.size() ; i++){
            authPolicies[i] = authPoliciesJsonArray.get(i).asString();
        }
        authResponse.setAuth_policies(authPolicies);
        authResponse.setAuth_lease_duration(authJsonObject.getInt("lease_duration",0));
        authResponse.setAuth_renewable(authJsonObject.getBoolean("renewable",false));
        authResponse.setApp_id(authJsonObject.get("metadata").asObject().getString("app-id",""));
        authResponse.setUser_id(authJsonObject.get("metadata").asObject().getString("user-id",""));
        authResponse.setUsername(authJsonObject.get("metadata").asObject().getString("username",""));
        return authResponse;
    }
}
