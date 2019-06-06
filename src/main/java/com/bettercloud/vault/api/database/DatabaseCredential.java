package com.bettercloud.vault.api.database;

public class DatabaseCredential {

    private String username;
    private String password;

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

    public DatabaseCredential username(String username) {
        this.username = username;
        return this;
    }

    public DatabaseCredential password(String password) {
        this.password = password;
        return this;
    }
}
