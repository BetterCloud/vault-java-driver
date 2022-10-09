package io.github.jopenlibs.vault.v1_1_3.api;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.HealthResponse;
import io.github.jopenlibs.vault.v1_1_3.util.VaultContainer;
import java.io.IOException;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

/**
 * <p>Integration tests for the debug-related operations on the Vault HTTP API's.</p>
 */
public class DebugTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    private Vault vault;

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
    }

    @Before
    public void setup() throws VaultException {
        vault = container.getRootVault();
    }

    @Test
    public void testHealth_Plain() throws VaultException {
        final HealthResponse response = vault.debug().health();

        assertTrue(response.getInitialized());
        assertFalse(response.getSealed());
        assertFalse(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        TestCase.assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testHealth_WithParams() throws VaultException {
        final HealthResponse response = vault.debug().health(null, 212, null, null);
        assertTrue(response.getInitialized());
        assertFalse(response.getSealed());
        assertFalse(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        TestCase.assertEquals(212, response.getRestResponse().getStatus());
    }

    /**
     * <p>Altering the default HTTP status codes with optional parameters can cause Vault to return an empty JSON
     * payload, depending on which replacement HTTP status code you specify.</p>
     *
     * <p>For example... Vault still returns a valid JSON payload when you change activeCode to 212 (see test above),
     * but returns an empty payload when you set it to use 204.</p>
     *
     * @throws VaultException
     */
    @Test
    public void testHealth_WonkyActiveCode() throws VaultException {
        final HealthResponse response = vault.debug().health(null, 204, null,
                null);
        assertNull(response.getInitialized());
        assertNull(response.getSealed());
        assertNull(response.getStandby());
        assertNull(response.getServerTimeUTC());
        TestCase.assertEquals(204, response.getRestResponse().getStatus());
    }
}
