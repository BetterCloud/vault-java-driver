package com.bettercloud.vault;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

public class VaultConfigTests {

    class MockEnvironmentLoader extends VaultConfig.EnvironmentLoader {
        final Map<String, String> overrides = new HashMap<String, String>();

        public void override(final String name, final String value) {
            this.overrides.put(name, value);
        }

        @Override
        public String loadVariable(final String name) {
            return overrides.get(name);
        }

    }

    @Test
    public void testConfigConstructor() throws VaultException {
        final VaultConfig config = new VaultConfig("address", "token");
        assertEquals("address", config.getAddress());
        assertEquals("token", config.getToken());
    }

    @Test
    public void testConfigConstructor_LoadFromEnv() throws VaultException {
        final MockEnvironmentLoader mock = new MockEnvironmentLoader();
        mock.override("VAULT_ADDR", "mock");
        mock.override("VAULT_TOKEN", "mock");

        final VaultConfig config = new VaultConfig(null, null, mock);
        assertEquals("mock", config.getAddress());
        assertEquals("mock", config.getToken());
    }

    @Test(expected = VaultException.class)
    public void testConfigConstructor_FailToLoad() throws VaultException {
        final VaultConfig config = new VaultConfig(null, null);
    }

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

    @Test
    public void testConfigBuilder_LoadFromEnv() throws VaultException {
        final MockEnvironmentLoader mock = new MockEnvironmentLoader();
        mock.override("VAULT_ADDR", "mock");
        mock.override("VAULT_TOKEN", "mock");

        final VaultConfig config = new VaultConfig()
                .environmentLoader(mock)
                .build();
        assertEquals("mock", config.getAddress());
        assertEquals("mock", config.getToken());
    }

    @Test(expected = VaultException.class)
    public void testConfigBuilder_FailToLoad() throws VaultException {
        final VaultConfig config = new VaultConfig()
                .build();
    }

}
