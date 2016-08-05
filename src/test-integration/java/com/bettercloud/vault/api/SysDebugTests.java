package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.HealthResponse;
import org.junit.ClassRule;
import org.junit.Test;

import static junit.framework.TestCase.*;

/**
 * <p>Integration tests for the debug-related operations on the Vault HTTP API's
 * <code>/v1/sys/*</code> endpoints.</p>
 *
 * <p>These tests require a Vault server to be up and running.  See the setup notes in
 * "src/test-integration/README.md".</p>
 */
public class SysDebugTests {

    final static String rootToken = "36303304-3f53-a0c9-af5d-3ffc8dabe683";

    @ClassRule
    public static final VaultContainer container = new VaultContainer(rootToken);

    @Test
    public void testHealth_Plain() throws VaultException {
        final Vault vault = container.getRootVault();
        final HealthResponse response = vault.sys().debug().health();
        assertTrue(response.getInitialized());
        assertFalse(response.getSealed());
        assertFalse(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testHealth_WithToken() throws VaultException {
        final Vault localVault = container.getRootVault();
        final HealthResponse response = localVault.sys().debug().health();
        assertTrue(response.getInitialized());
        assertFalse(response.getSealed());
        assertFalse(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testHealth_WithParams() throws VaultException {
        final Vault vault = container.getRootVault();
        final HealthResponse response = vault.sys().debug().health(null, 212, null, null);
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
        final Vault vault = container.getRootVault();
        final HealthResponse response = vault.sys().debug().health(null, 204, null, null);
        assertNull(response.getInitialized());
        assertNull(response.getSealed());
        assertNull(response.getStandby());
        assertNull(response.getServerTimeUTC());
        assertEquals(204, response.getRestResponse().getStatus());
    }
}
