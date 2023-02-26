package io.github.jopenlibs.vault.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.util.VaultContainer;
import io.github.jopenlibs.vault.util.VaultVersion;
import java.io.IOException;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assume.assumeTrue;

/**
 * Integration tests for the AppId auth backend. According to the Vault documentation, this method
 * is deprecated. Also, from Vault 1.11.7 it fails the usage. For this reason, we skip this test if
 * Vault version is greater than 1.11.6
 */
public class AuthBackendAppIdTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    private static boolean checkVersion() {
        VaultVersion accepted = new VaultVersion("1.11.6");
        try {
            VaultVersion current = new VaultVersion(
                    Optional.ofNullable(System.getenv("VAULT_VERSION")).orElse("latest"));
            if (current.compareTo(accepted) > 0) {
                return false;
            }
        } catch (NumberFormatException ignored) {
            return false;
        }

        return true;
    }

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        assumeTrue(checkVersion());

        container.initAndUnsealVault();
        container.setupBackendAppId();
    }


    /**
     * Test Authentication with app-id auth backend
     */
    @Test
    public void testLoginByAuthId() throws VaultException {
        final Vault vault = container.getVault();
        final String path = "app-id/login";
        @SuppressWarnings("deprecation") // used for testing
        final String token = vault.auth()
                .loginByAppID(path, VaultContainer.APP_ID, VaultContainer.USER_ID)
                .getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());
    }

}
