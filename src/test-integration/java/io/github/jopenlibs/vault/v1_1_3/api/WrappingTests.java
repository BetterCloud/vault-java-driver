package io.github.jopenlibs.vault.v1_1_3.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.response.AuthResponse;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.response.UnwrapResponse;
import io.github.jopenlibs.vault.response.WrapResponse;
import io.github.jopenlibs.vault.v1_1_3.util.VaultContainer;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the functions work with {@code /sys/wrapping/*} endpoints.
 */
public class WrappingTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    private static String NONROOT_TOKEN;

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException, VaultException {
        container.initAndUnsealVault();
        container.setupUserPassWithAllowRewrap();

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

        assertEquals("bar", unwrapResponse.getData().get("foo").asString());
        assertEquals("zar", unwrapResponse.getData().get("zoo").asString());
    }

    /**
     * Tests endpoints: /sys/wrapping/wrap, /sys/wrapping/lookup, /sys/wrapping/unwrap,
     * /sys/wrapping/rewrap.
     */
    @Test
    public void testWrappingAll() throws Exception {
        final Vault vault = container.getVault(NONROOT_TOKEN);

        WrapResponse wrapResponse0 = vault.auth().wrap(
                new JsonObject()
                        .add("foo", "bar")
                        .add("zoo", "zar"),
                60
        );

        LogicalResponse look = vault.auth().lookupWrap(wrapResponse0.getToken());

        assertNotNull(look.getData().get("creation_time"));
        assertNotNull(look.getData().get("creation_ttl"));
        assertEquals("sys/wrapping/wrap", look.getData().get("creation_path"));

        WrapResponse wrapResponse1 = vault.auth().rewrap(wrapResponse0.getToken());

        VaultException ex = assertThrows(
                VaultException.class,
                () -> vault.auth().unwrap(wrapResponse0.getToken())
        );
        assertTrue(ex.getMessage().contains("wrapping token is not valid or does not exist"));

        UnwrapResponse unwrapResponse = vault.auth().unwrap(wrapResponse1.getToken());

        assertEquals("bar", unwrapResponse.getData().get("foo").asString());
        assertEquals("zar", unwrapResponse.getData().get("zoo").asString());
    }
}
