package com.bettercloud.vault;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Unit tests for the <code>VaultConfig</code> settings loader.
 */
public class VaultConfigTests {

    /**
     * <p>The code used by <code>VaultConfig</code> to load environment variables is encapsulated within an inner
     * class, so that a mock version of that environment loader can be used by unit tests.</p>
     *
     * <p>This mock implementation of <code>VaultConfig.EnvironmentLoader</code> allows unit tests to declare values
     * that should be returned for a given environment variable name.  The actual environment is never used.
     */
    class MockEnvironmentLoader extends VaultConfig.EnvironmentLoader {
        final Map<String, String> overrides = new HashMap<String, String>();

        /**
         * Declare a variable and value to be available in the mock "environment".  This method may be called
         * repeatedly, to populate multiple variables.  This method should be called prior to passing the object
         * instance to a <code>VaultConfig</code> constructor, or calling the <code>build()</code> method on that class.
         *
         * @param name Mock environment variable name
         * @param value Mock environment variable value
         */
        public void override(final String name, final String value) {
            this.overrides.put(name, value);
        }

        @Override
        public String loadVariable(final String name) {
            return overrides.get(name);
        }

    }

    /**
     * Test creating a new <code>VaultConfig</code> via its constructor, passing address and token values and ensuring
     * that they're later accessible.
     *
     * @throws VaultException
     */
    @Test
    public void testConfigConstructor() throws VaultException {
        final VaultConfig config = new VaultConfig("address", "token");
        assertEquals("address", config.getAddress());
        assertEquals("token", config.getToken());
    }

    /**
     * Test creating a new <code>VaultConfig</code> via its constructor, deliberately passing null address and token
     * values so it's forced to fetch them from environment variables.
     *
     * @throws VaultException
     */
    @Test
    public void testConfigConstructor_LoadFromEnv() throws VaultException {
        final MockEnvironmentLoader mock = new MockEnvironmentLoader();
        // Required
        mock.override("VAULT_ADDR", "http://127.0.0.1:8200");
        mock.override("VAULT_TOKEN", "c24e2469-298a-6c64-6a71-5b47c9ba459a");
        // Optional
        mock.override("VAULT_PROXY_ADDRESS", "localhost");
        mock.override("VAULT_PROXY_PORT", "80");
        mock.override("VAULT_PROXY_USERNAME", "scott");
        mock.override("VAULT_PROXY_PASSWORD", "tiger");
        mock.override("VAULT_SSL_CERT", "mycert.pem");
        mock.override("VAULT_SSL_VERIFY", "true");
        mock.override("VAULT_TIMEOUT", "30");
        mock.override("VAULT_SSL_TIMEOUT", "30");
        mock.override("VAULT_OPEN_TIMEOUT", "30");
        mock.override("VAULT_READ_TIMEOUT", "30");

        final VaultConfig config = new VaultConfig(null, null, mock);
        assertEquals("http://127.0.0.1:8200", config.getAddress());
        assertEquals("c24e2469-298a-6c64-6a71-5b47c9ba459a", config.getToken());
        assertEquals("localhost", config.getProxyAddress());
        assertTrue(80 == config.getProxyPort());
        assertEquals("scott", config.getProxyUsername());
        assertEquals("tiger", config.getProxyPassword());
        assertEquals("mycert.pem", config.getSslPemFile());
        assertTrue(config.isSslVerify());
        assertTrue(30 == config.getTimeout());
        assertTrue(30 == config.getSslTimeout());
        assertTrue(30 == config.getOpenTimeout());
        assertTrue(30 == config.getReadTimeout());
    }

    /**
     * Test creating a new <code>VaultConfig</code> via its constructor, passing null address and token values AND
     * having them unavailable in the environment variables too.  This should cause initialization failure.
     *
     * @throws VaultException
     */
    @Test(expected = VaultException.class)
    public void testConfigConstructor_FailToLoad() throws VaultException {
        final VaultConfig config = new VaultConfig(null);
    }

    /**
     * Test creating a <code>VaultConfig</code> instance via its builder pattern, explicitly specifying address and
     * token values.
     *
     * @throws VaultException
     */
    @Test
    public void testConfigBuilder() throws VaultException {
        final VaultConfig config =
                new VaultConfig()
                        .address("address")
                        .token("token")
                        .build();
        assertEquals("address", config.getAddress());
        assertEquals("token", config.getToken());
    }

    /**
     * Test creating a <code>VaultConfig</code> instance via its builder pattern, forcing it to look to the environment
     * variables for address and token values.
     *
     * @throws VaultException
     */
    @Test
    public void testConfigBuilder_LoadFromEnv() throws VaultException {
        final MockEnvironmentLoader mock = new MockEnvironmentLoader();
        mock.override("VAULT_ADDR", "http://127.0.0.1:8200");
        mock.override("VAULT_TOKEN", "c24e2469-298a-6c64-6a71-5b47c9ba459a");
        mock.override("VAULT_PROXY_ADDRESS", "localhost");
        mock.override("VAULT_PROXY_PORT", "80");
        mock.override("VAULT_PROXY_USERNAME", "scott");
        mock.override("VAULT_PROXY_PASSWORD", "tiger");
        mock.override("VAULT_SSL_CERT", "mycert.pem");
        mock.override("VAULT_SSL_VERIFY", "true");
        mock.override("VAULT_TIMEOUT", "30");
        mock.override("VAULT_SSL_TIMEOUT", "30");
        mock.override("VAULT_OPEN_TIMEOUT", "30");
        mock.override("VAULT_READ_TIMEOUT", "30");

        final VaultConfig config = new VaultConfig()
                .environmentLoader(mock)
                .build();
        assertEquals("http://127.0.0.1:8200", config.getAddress());
        assertEquals("c24e2469-298a-6c64-6a71-5b47c9ba459a", config.getToken());
        assertEquals("localhost", config.getProxyAddress());
        assertTrue(80 == config.getProxyPort());
        assertEquals("scott", config.getProxyUsername());
        assertEquals("tiger", config.getProxyPassword());
        assertEquals("mycert.pem", config.getSslPemFile());
        assertTrue(config.isSslVerify());
        assertTrue(30 == config.getTimeout());
        assertTrue(30 == config.getSslTimeout());
        assertTrue(30 == config.getOpenTimeout());
        assertTrue(30 == config.getReadTimeout());
    }

    /**
     * Test creating a <code>VaultConfig</code> instance via its builder pattern, with no address no token values
     * passed OR available in the environment.  This should cause initialization failure.
     *
     * @throws VaultException
     */
    @Test(expected = VaultException.class)
    public void testConfigBuilder_FailToLoad() throws VaultException {
        final VaultConfig config = new VaultConfig().build();
    }

}
