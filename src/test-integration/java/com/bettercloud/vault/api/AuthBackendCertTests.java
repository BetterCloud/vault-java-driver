package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/** Integration tests for the AppId auth backend. */
public class AuthBackendCertTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
        container.setupCertBackend();
    }

    /** Test Authentication with TLS cert auth backend */
    @Test
    public void testLoginByCert() throws VaultException {
        final Vault vault = container.getVault();
//        final Vault vault = container.getRootVault();

        final String token = vault.auth().loginByCert().getAuthClientToken();

        assertNotNull(token);
        assertNotSame("", token.trim());
    }

}
