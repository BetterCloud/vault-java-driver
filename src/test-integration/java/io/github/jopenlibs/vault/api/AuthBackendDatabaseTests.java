package io.github.jopenlibs.vault.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.database.DatabaseRoleOptions;
import io.github.jopenlibs.vault.response.DatabaseResponse;
import io.github.jopenlibs.vault.util.DbContainer;
import io.github.jopenlibs.vault.util.VaultContainer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class AuthBackendDatabaseTests {

    @ClassRule
    public static final DbContainer dbContainer = new DbContainer();

    @ClassRule
    public static final VaultContainer container = new VaultContainer().dependsOn(dbContainer);


    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
        container.setupBackendDatabase(DbContainer.hostname);
    }

    @Test
    public void testRoleCreation() throws VaultException {
        final Vault vault = container.getRootVault();

        List<String> creationStatements = new ArrayList<>();
        creationStatements.add(
                "CREATE USER \"{{name}}\" WITH PASSWORD '{{password}}'; GRANT ALL PRIVILEGES ON DATABASE \"postgres\" to \"{{name}}\";");

        DatabaseRoleOptions roleToCreate = new DatabaseRoleOptions().dbName("postgres")
                .creationStatements(creationStatements);

        DatabaseResponse response = vault.database().createOrUpdateRole("test-role", roleToCreate);
        TestCase.assertEquals(204, response.getRestResponse().getStatus());

        DatabaseResponse role = vault.database().getRole("test-role");
        TestCase.assertEquals(200, role.getRestResponse().getStatus());

        assertTrue(compareRoleOptions(role.getRoleOptions(), roleToCreate));
    }

    @Test
    public void testDeleteRole() throws VaultException {
        final Vault vault = container.getRootVault();

        List<String> creationStatements = new ArrayList<>();
        creationStatements.add(
                "CREATE USER \"{{name}}\" WITH PASSWORD '{{password}}'; GRANT ALL PRIVILEGES ON DATABASE \"postgres\" to \"{{name}}\";");

        DatabaseRoleOptions roleToCreate = new DatabaseRoleOptions().dbName("postgres")
                .creationStatements(creationStatements);

        DatabaseResponse response = vault.database()
                .createOrUpdateRole("delete-role", roleToCreate);
        TestCase.assertEquals(204, response.getRestResponse().getStatus());

        DatabaseResponse deletedRole = vault.database().deleteRole("delete-role");
        TestCase.assertEquals(204, deletedRole.getRestResponse().getStatus());

        try {
            DatabaseResponse role = vault.database().getRole("delete-role");
        } catch (VaultException e) {
            assertEquals("This should have failed", 404, e.getHttpStatusCode());
        }
    }

    @Test
    public void testRoleNotFound() throws VaultException {
        final Vault vault = container.getRootVault();

        try {
            DatabaseResponse role = vault.database().getRole("i-do-not-exist");
        } catch (VaultException e) {
            assertEquals("This should have failed", 404, e.getHttpStatusCode());
        }
    }

    @Test
    public void testGetCredentials() throws VaultException {
        final Vault vault = container.getRootVault();

        List<String> creationStatements = new ArrayList<>();
        creationStatements.add(
                "CREATE USER \"{{name}}\" WITH PASSWORD '{{password}}'; GRANT ALL PRIVILEGES ON DATABASE \"postgres\" to \"{{name}}\";");

        DatabaseResponse response = vault.database().createOrUpdateRole("new-role",
                new DatabaseRoleOptions().dbName("postgres")
                        .creationStatements(creationStatements));
        TestCase.assertEquals(204, response.getRestResponse().getStatus());

        DatabaseResponse credsResponse = vault.database().creds("new-role");
        TestCase.assertEquals(200, credsResponse.getRestResponse().getStatus());

        TestCase.assertTrue(credsResponse.getCredential().getUsername().contains("new-role"));
    }

    private boolean compareRoleOptions(DatabaseRoleOptions expected, DatabaseRoleOptions actual) {
        return expected.getCreationStatements().size() == actual.getCreationStatements().size() &&
                expected.getRenewStatements().size() == actual.getRenewStatements().size() &&
                expected.getRevocationStatements().size() == actual.getRevocationStatements().size()
                &&
                expected.getRollbackStatements().size() == actual.getRollbackStatements().size();
    }
}
