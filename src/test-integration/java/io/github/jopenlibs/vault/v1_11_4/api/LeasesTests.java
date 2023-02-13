package io.github.jopenlibs.vault.v1_11_4.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.database.DatabaseRoleOptions;
import io.github.jopenlibs.vault.response.DatabaseResponse;
import io.github.jopenlibs.vault.response.VaultResponse;
import io.github.jopenlibs.vault.v1_11_4.util.DbContainer;
import io.github.jopenlibs.vault.v1_11_4.util.VaultContainer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * <p>Integration tests for the leases (i.e. "sys/leases") Vault API operations.</p>
 *
 * According to the Vault documentation, it is possible to use a dynamic secret like database to
 * test these methods
 */
public class LeasesTests {

    @ClassRule
    public static final DbContainer dbContainer = new DbContainer();

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    private Vault vault;

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
        container.setupBackendDatabase(DbContainer.hostname);
    }

    @Before
    public void setup() throws VaultException {
        vault = container.getRootVault();
    }

    public DatabaseResponse generateCredentials() throws VaultException {
        List<String> creationStatements = new ArrayList<>();
        creationStatements.add(
                "CREATE USER \"{{name}}\" WITH PASSWORD '{{password}}';"
                        + "GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";");

        DatabaseResponse databaseResponse = vault.database().createOrUpdateRole("new-role",
                new DatabaseRoleOptions().dbName("postgres")
                        .creationStatements(creationStatements));
        assertEquals(204, databaseResponse.getRestResponse().getStatus());

        DatabaseResponse credsResponse = vault.database().creds("new-role");
        assertEquals(200, credsResponse.getRestResponse().getStatus());

        assertTrue(credsResponse.getCredential().getUsername().contains("new-role"));

        return credsResponse;
    }

    @Test
    public void testRevoke() throws VaultException {
        DatabaseResponse credsResponse = this.generateCredentials();

        final VaultResponse response = vault.leases().revoke(credsResponse.getLeaseId());
        assertEquals(204, response.getRestResponse().getStatus());
    }

    @Test
    public void testRevokePrefix() throws VaultException {
        DatabaseResponse credsResponse = this.generateCredentials();

        String prefix = Arrays.stream(credsResponse.getLeaseId().split("([^/]+)$"))
                .map(str -> str.substring(0, str.length() - 1)).findFirst().get();

        final VaultResponse response = vault.leases().revokePrefix(prefix);
        assertEquals(204, response.getRestResponse().getStatus());
    }

    @Test
    public void testRevokeForce() throws VaultException {
        DatabaseResponse credsResponse = this.generateCredentials();

        String prefix = Arrays.stream(credsResponse.getLeaseId().split("([^/]+)$"))
                .map(str -> str.substring(0, str.length() - 1)).findFirst().get();

        final VaultResponse response = vault.leases().revokeForce(prefix);
        assertEquals(204, response.getRestResponse().getStatus());
    }

    @Test
    public void testRenew() throws VaultException {
        DatabaseResponse credsResponse = this.generateCredentials();

        final VaultResponse response = vault.leases().renew(credsResponse.getLeaseId(),
                credsResponse.getLeaseDuration());
        assertEquals(200, response.getRestResponse().getStatus());
    }
}
