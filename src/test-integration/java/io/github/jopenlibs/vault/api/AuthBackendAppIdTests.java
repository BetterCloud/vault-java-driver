package io.github.jopenlibs.vault.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.util.VaultContainer;
import io.github.jopenlibs.vault.util.VaultVersion;
import java.io.IOException;
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

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        assumeTrue(VaultVersion.lessThan("1.11.6"));

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
