package com.bettercloud.vault.api.database;

public class DatabaseCredential {

    private String username;
    private String password;
    private String leaseId;
    private String leaseDuration;
    private Boolean leaseRenewable;

    /**
     * @return Returns the Username associated with the retrieved Credential
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return Returns the Password associated with the retrieved Credential
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return Returns the lease id associated with the retrieved Credential
     */
    public String getLeaseId() {
        return leaseId;
    }

    /**
     * @return Returns the lease duration associated with the retrieved Credential
     */
    public String getLeaseDuration() {
        return leaseDuration;
    }

    /**
     * @return Returns the lease duration associated with the retrieved Credential
     */
    public Boolean getLeaseRenewable() {
        return leaseRenewable;
    }

    public DatabaseCredential username(String username) {
        this.username = username;
        return this;
    }

    public DatabaseCredential password(String password) {
        this.password = password;
        return this;
    }

    public DatabaseCredential leaseId(String leaseId) {
        this.leaseId = leaseId;
        return this;
    }

    public DatabaseCredential leaseDuration(String leaseDuration) {
        this.leaseDuration = leaseDuration;
        return this;
    }

    public DatabaseCredential leaseRenewable(Boolean leaseRenewable) {
        this.leaseRenewable = leaseRenewable;
        return this;
    }
}
