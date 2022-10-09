package io.github.jopenlibs.vault.v1_1_3.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.response.AuthResponse;
import io.github.jopenlibs.vault.response.UnwrapResponse;
import io.github.jopenlibs.vault.response.WrapResponse;
import io.github.jopenlibs.vault.v1_1_3.util.VaultContainer;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for the wrap/unwrap data.
 */
public class WrapUnwrapTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    private static String NONROOT_TOKEN;

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException, VaultException {
        container.initAndUnsealVault();
        container.setupBackendUserPass();

        final Vault vault = container.getVault();
        final AuthResponse response = vault.auth()
                .loginByUserPass(VaultContainer.USER_ID, VaultContainer.PASSWORD);

        NONROOT_TOKEN = response.getAuthClientToken();
    }

    /**
     * Tests wrap/unwrap data.
     */
    @Test
    public void testWrapUnwrap() throws Exception {
        final Vault vault = container.getVault(NONROOT_TOKEN);

        WrapResponse wrapResponse = vault.auth().wrap(
                new JsonObject()
                        .add("foo", "bar")
                        .add("zoo", "zar"),
                60
        );

        UnwrapResponse unwrapResponse = vault.auth().unwrap(wrapResponse.getToken());

        assertEquals("bar", unwrapResponse.getData().asObject().get("foo").asString());
        assertEquals("zar", unwrapResponse.getData().asObject().get("zoo").asString());
    }
}
