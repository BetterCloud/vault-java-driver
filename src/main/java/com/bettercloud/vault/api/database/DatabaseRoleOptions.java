package com.bettercloud.vault.api.database;

import java.util.ArrayList;
import java.util.List;

public class DatabaseRoleOptions {
    private String name;
    private String dbName;
    private String defaultTtl = "0";
    private String maxTtl = "0";
    private List<String> creationStatements = new ArrayList<>();
    private List<String> revocationStatements = new ArrayList<>();
    private List<String> rollbackStatements = new ArrayList<>();
    private List<String> renewStatements = new ArrayList<>();

    public String getName() {
        return name;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDefaultTtl() {
        return defaultTtl;
    }

    public String getMaxTtl() {
        return maxTtl;
    }

    public List<String> getCreationStatements() {
        return creationStatements;
    }

    public List<String> getRenewStatements() {
        return renewStatements;
    }

    public List<String> getRevocationStatements() {
        return revocationStatements;
    }

    public List<String> getRollbackStatements() {
        return rollbackStatements;
    }

    /**
     * @param name {@code String} – Specifies the name of the role to create. This is specified as part of the URL.
     * @return This object, with name populated, ready for other builder methods or immediate use.
     */
    public DatabaseRoleOptions name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param dbName {@code String} - The name of the database connection to use for this role.
     * @return This object, with dbName populated, ready for other builder methods or immediate use.
     */
    public DatabaseRoleOptions dbName(final String dbName) {
        this.dbName = dbName;
        return this;
    }

    /**
     * @param defaultTtl (string/int: 0) - Specifies the TTL for the leases associated with this role. Accepts time suffixed strings ("1h") or an integer number of seconds. Defaults to system/engine default TTL time.
     * @return This object, with defaultTtl populated, ready for other builder methods or immediate use.
     */
    public DatabaseRoleOptions defaultTtl(final String defaultTtl) {
        this.defaultTtl = defaultTtl;
        return this;
    }

    /**
     * @param maxTtl (string/int: 0) - Specifies the maximum TTL for the leases associated with this role. Accepts time suffixed strings ("1h") or an integer number of seconds. Defaults to system/mount default TTL time; this value is allowed to be less than the mount max TTL (or, if not set, the system max TTL), but it is not allowed to be longer. See also The TTL General Case.
     * @return This object, with maxTtl populated, ready for other builder methods or immediate use.
     */
    public DatabaseRoleOptions maxTtl(final String maxTtl) {
        this.maxTtl = maxTtl;
        return this;
    }

    /**
     * @param creationStatements {@code List<String>} – Specifies the database statements executed to create and configure a user. See the plugin's API page for more information on support and formatting for this parameter.
     * @return This object, with creationStatements populated, ready for other builder methods or immediate use.
     */
    public DatabaseRoleOptions creationStatements(final List<String> creationStatements) {
        this.creationStatements = creationStatements;
        return this;
    }

    /**
     * @param revocationStatements (list: []) – Specifies the database statements to be executed to revoke a user. See the plugin's API page for more information on support and formatting for this parameter.
     * @return This object, with revocationStatements populated, ready for other builder methods or immediate use.
     */
    public DatabaseRoleOptions revocationStatements(final List<String> revocationStatements) {
        this.revocationStatements = revocationStatements;
        return this;
    }

    /**
     * @param rollbackStatements (list: []) – Specifies the database statements to be executed rollback a create operation in the event of an error. Not every plugin type will support this functionality. See the plugin's API page for more information on support and formatting for this parameter.
     * @return This object, with rollbackStatements populated, ready for other builder methods or immediate use.
     */
    public DatabaseRoleOptions rollbackStatements(final List<String> rollbackStatements) {
        this.rollbackStatements = rollbackStatements;
        return this;
    }

    /**
     * @param renewStatements (list: []) – Specifies the database statements to be executed to renew a user. Not every plugin type will support this functionality. See the plugin's API page for more information on support and formatting for this parameter.
     * @return This object, with renewStatements populated, ready for other builder methods or immediate use.
     */
    public DatabaseRoleOptions renewStatements(final List<String> renewStatements) {
        this.renewStatements = renewStatements;
        return this;
    }
}
