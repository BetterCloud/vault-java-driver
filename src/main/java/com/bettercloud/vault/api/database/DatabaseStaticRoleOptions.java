package com.bettercloud.vault.api.database;

import java.util.ArrayList;
import java.util.List;

public class DatabaseStaticRoleOptions {
    private String name;
    private String dbName;
    private String username;
    private String rotationPeriod;
    private List<String> rotationStatements = new ArrayList<>();

    public String getName() {
        return name;
    }

    public String getDbName() {
        return dbName;
    }

    public String getUsername() {
        return username;
    }

    public String getRotationPeriod() {
        return rotationPeriod;
    }

    public List<String> getRotationStatements() {
        return rotationStatements;
    }

    /**
     * @param name {@code String} – Specifies the name of the role to create. This is specified as part of the URL.
     * @return This object, with name populated, ready for other builder methods or immediate use.
     */
    public DatabaseStaticRoleOptions name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param dbName {@code String} - The name of the database connection to use for this role.
     * @return This object, with dbName populated, ready for other builder methods or immediate use.
     */
    public DatabaseStaticRoleOptions dbName(final String dbName) {
        this.dbName = dbName;
        return this;
    }

    /**
     * @param username {@code String} - The database usernameto use for this role.
     * @return This object, with dbName populated, ready for other builder methods or immediate use.
     */
    public DatabaseStaticRoleOptions username(final String username) {
        this.username = username;
        return this;
    }

    /**
     * @param rotationPeriod (string/int: 0) - Specifies the amount of time Vault should wait before rotating the password. The minimum is 5 seconds.
     * @return This object, with defaultTtl populated, ready for other builder methods or immediate use.
     */
    public DatabaseStaticRoleOptions rotationPeriod(final String rotationPeriod) {
        this.rotationPeriod = rotationPeriod;
        return this;
    }

    /**
     * @param rotationStatements {@code List<String>} – Specifies the database statements to be executed to rotate the password for the configured database user. Not every plugin type will support this functionality. See the plugin's API page for more information on support and formatting for this parameter.
     * @return This object, with creationStatements populated, ready for other builder methods or immediate use.
     */
    public DatabaseStaticRoleOptions rotationStatements(final List<String> rotationStatements) {
        this.rotationStatements = rotationStatements;
        return this;
    }

}
