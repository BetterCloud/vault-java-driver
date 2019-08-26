package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.util.VaultContainer;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/**
 * Integration tests for the AppId auth backend.
 */
public class AuthBackendAppIdTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
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
        final String token = vault.auth().loginByAppID(path, VaultContainer.APP_ID, VaultContainer.USER_ID)
                .getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());
    }

}
