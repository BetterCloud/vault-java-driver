package com.bettercloud.vault;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class VaultConfigTests {

    class MockEnvironmentLoader extends VaultConfig.EnvironmentLoader {
        @Override
        public String loadVariable(final String name) {
            return "mock";
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
        final VaultConfig config = new VaultConfig(null, null, new MockEnvironmentLoader());
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
        final VaultConfig config = new VaultConfig()
                .environmentLoader(new MockEnvironmentLoader())
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
