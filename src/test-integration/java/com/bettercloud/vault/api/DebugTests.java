package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.HealthResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.TestCase.*;

/**
 * <p>Integration tests for the debug-related operations on the Vault HTTP API's.</p>
 *
 * <p>These tests require a Vault server to be up and running.  See the setup notes in
 * "src/test-integration/README.md".</p>
 */
public class DebugTests {

    private static final String address = System.getProperty("VAULT_ADDR");
    private static final String token = System.getProperty("VAULT_TOKEN");

    private Vault vault;

    @BeforeClass
    public static void verifyEnv() {
        assertNotNull(address);
        assertNotNull(token);
    }

    @Before
    public void setup() throws VaultException {
        final VaultConfig config = new VaultConfig(address);
        vault = new Vault(config);
    }

    @Test
    public void testHealth_Plain() throws VaultException {
        final HealthResponse response = vault.debug().health();
        assertTrue(response.getInitialized());
        assertFalse(response.getSealed());
        assertFalse(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testHealth_WithToken() throws VaultException {
        final Vault localVault = new Vault(new VaultConfig(address, token));
        final HealthResponse response = localVault.debug().health();
        assertTrue(response.getInitialized());
        assertFalse(response.getSealed());
        assertFalse(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testHealth_WithParams() throws VaultException {
        final HealthResponse response = vault.debug().health(null, 212, null, null);
        assertTrue(response.getInitialized());
        assertFalse(response.getSealed());
        assertFalse(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        assertEquals(212, response.getRestResponse().getStatus());
    }

    /**
     * <p>Altering the default HTTP status codes with optional parameters can cause Vault to return an empty JSON
     * payload, depending on which replacement HTTP status code you specify.</p>
     *
     * <p>For example... Vault still returns a valid JSON payload when you change activeCode to 212 (see test above),
     * but returns an empty payload when you set it to use 204.</p>
     * @throws VaultException
     */
    @Test
    public void testHealth_WonkyActiveCode() throws VaultException {
        final HealthResponse response = vault.debug().health(null, 204, null, null);
        assertNull(response.getInitialized());
        assertNull(response.getSealed());
        assertNull(response.getStandby());
        assertNull(response.getServerTimeUTC());
        assertEquals(204, response.getRestResponse().getStatus());
    }
}
