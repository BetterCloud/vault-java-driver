package com.bettercloud.vault.response;

/**
 * Created by amit on 1/21/16.
 */
public class AuthResponse {
    private String lease_id;
    private boolean renewable;
    private int lease_duration;
    private String auth_client_token;
    private String[] auth_policies;
    private int auth_lease_duration;
    private boolean auth_renewable;
    private String app_id;
    private String user_id;
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getLease_id() {
        return lease_id;
    }

    public void setLease_id(final String lease_id) {
        this.lease_id = lease_id;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public void setRenewable(final boolean renewable) {
        this.renewable = renewable;
    }

    public int getLease_duration() {
        return lease_duration;
    }

    public void setLease_duration(final int lease_duration) {
        this.lease_duration = lease_duration;
    }

    public String getAuth_client_token() {
        return auth_client_token;
    }

    public void setAuth_client_token(final String auth_client_token) {
        this.auth_client_token = auth_client_token;
    }

    public String[] getAuth_policies() {
        return auth_policies;
    }

    public void setAuth_policies(final String[] auth_policies) {
        this.auth_policies = auth_policies;
    }

    public int getAuth_lease_duration() {
        return auth_lease_duration;
    }

    public void setAuth_lease_duration(final int auth_lease_duration) {
        this.auth_lease_duration = auth_lease_duration;
    }

    public boolean isAuth_renewable() {
        return auth_renewable;
    }

    public void setAuth_renewable(final boolean auth_renewable) {
        this.auth_renewable = auth_renewable;
    }

    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(final String app_id) {
        this.app_id = app_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(final String user_id) {
        this.user_id = user_id;
    }

}
