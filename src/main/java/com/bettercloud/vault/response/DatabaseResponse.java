package com.bettercloud.vault.response;

import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.api.database.DatabaseCredential;
import com.bettercloud.vault.api.database.DatabaseRoleOptions;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.rest.RestResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseResponse extends LogicalResponse {

    private DatabaseCredential credential;

    private DatabaseRoleOptions roleOptions;

    public DatabaseCredential getCredential() {
        return credential;
    }

    public DatabaseRoleOptions getRoleOptions() {
        return roleOptions;
    }

    /**
     * @param restResponse The raw HTTP response from Vault.
     * @param retries      The number of retry attempts that occurred during the API call (can be zero).
     */
    public DatabaseResponse(RestResponse restResponse, int retries) {
        super(restResponse, retries, Logical.logicalOperations.authentication);
        credential = buildCredentialFromData(this.getData());
        roleOptions = buildRoleOptionsFromData(this.getData(), this.getDataObject());
    }

    private DatabaseRoleOptions buildRoleOptionsFromData(final Map<String, String> data, final JsonObject jsonObject) {
        if (data == null || data.size() == 0) {
            return null;
        }

        final List<String> creationStatements = extractFromJsonArray(safeGetJsonArray(jsonObject, "creation_statements"));
        final List<String> renewStatements = extractFromJsonArray(safeGetJsonArray(jsonObject, "renew_statements"));
        final List<String> revocationStatements = extractFromJsonArray(safeGetJsonArray(jsonObject, "revocation_statements"));
        final List<String> rollbackStatements = extractFromJsonArray(safeGetJsonArray(jsonObject, "rollback_statements"));

        final String dbName = data.get("db_name");
        final String defaultTtl = data.get("default_ttl");
        final String maxTtl = data.get("default_ttl");

        if (dbName == null && defaultTtl == null && maxTtl == null) {
            return null;
        }

        return new DatabaseRoleOptions()
                .creationStatements(creationStatements)
                .renewStatements(renewStatements)
                .revocationStatements(revocationStatements)
                .rollbackStatements(rollbackStatements)
                .dbName(dbName)
                .defaultTtl(defaultTtl)
                .maxTtl(maxTtl);
    }

    private DatabaseCredential buildCredentialFromData(final Map<String, String> data) {
        if (data == null) {
            return null;
        }
        final String username = data.get("username");
        final String password = data.get("password");

        if (username == null && password == null) {
            return null;
        }

        return new DatabaseCredential()
                .username(username)
                .password(password);
    }

    private JsonArray safeGetJsonArray(JsonObject source, String key) {
        if (source == null || source.get(key) == null || source.get(key) == null || !source.get(key).isArray()) {
            return new JsonArray();
        }

        return source.get(key).asArray();
    }

    private List<String> extractFromJsonArray(JsonArray array) {
        List<String> result = new ArrayList<>();

        array.forEach(entry -> result.add(entry.asString()));

        return result;
    }
}
