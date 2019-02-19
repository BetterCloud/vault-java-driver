package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.HealthResponse;
import com.bettercloud.vault.util.VaultContainer;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotEquals;

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
        assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testHealth_Sealed() throws VaultException {
        vault.seal().seal();
        final HealthResponse response = vault.debug().health();

        assertTrue(response.getInitialized());
        assertTrue(response.getSealed());
        assertTrue(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        assertEquals(503, response.getRestResponse().getStatus());
        assertFalse(response.getPerformanceStandby());
        assertNotEquals("disabled", response.getReplicationPerformanceMode());
        assertNotEquals("disabled", response.getReplicationDrMode());
        container.getRootVault().seal().unseal(container.getUnsealKey());
    }

    @Test
    public void testHealth_WithParams() throws VaultException {
        final HealthResponse response = vault.debug().health(null, 212, null, null, null, null, null, null);
        assertTrue(response.getInitialized());
        assertFalse(response.getSealed());
        assertFalse(response.getStandby());
        assertFalse(response.getPerformanceStandby());
        assertNotNull(response.getServerTimeUTC());
        assertEquals(212, response.getRestResponse().getStatus());
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
                null, null, null, null, null);
        assertNull(response.getInitialized());
        assertNull(response.getSealed());
        assertNull(response.getStandby());
        assertNull(response.getServerTimeUTC());
        assertNull(response.getPerformanceStandby());
        assertNull(response.getReplicationDrMode());
        assertNull(response.getReplicationPerformanceMode());
        assertEquals(204, response.getRestResponse().getStatus());
    }

    @Test
    public void testHealth_WonkySealedCode() throws VaultException {
        vault.seal().seal();
        final HealthResponse response = vault.debug().health(null, null, null,
                900, null, null, null, null);
        assertTrue(response.getInitialized());
        assertTrue(response.getSealed());
        assertTrue(response.getStandby());
        assertNotNull(response.getServerTimeUTC());
        assertFalse(response.getPerformanceStandby());
        assertNotNull(response.getReplicationDrMode());
        assertNotNull(response.getReplicationPerformanceMode());
        assertEquals(900, response.getRestResponse().getStatus());
        container.getRootVault().seal().unseal(container.getUnsealKey());
    }
}
