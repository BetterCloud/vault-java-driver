package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

/**
 * Integration test to run on KV Secrets Engine - Version 2
 * This test expects the vault is running with url http://localhost:8200 and enabled a token as "00000000-0000-0000-0000-000000000000" with read and write privilege.
 */
public class LogicalV2Tests {
    private static final String address = "http://localhost:8200";
    private static final String token = "00000000-0000-0000-0000-000000000000";
    private static final boolean isVerifySsl = false;
    private static Vault vault;

    @BeforeClass
    public static void setUp() throws VaultException {
        VaultConfig vaultConfig = new VaultConfig().address(address)
                .token(token).sslVerify(isVerifySsl).build();
        vault = new Vault(vaultConfig);
    }

    @Test
    public void testReadAndWrite() throws VaultException {
        final String path = "secret/place";
        final String key = "city";
        final String value_1 = "Bangalore";
        final String value_2 = "Denver";
        vault.logical().write(path, new HashMap<String, String>() {{
            put(key, value_1);
        }});
        Assert.assertEquals(value_1, vault.logical().read(path).getData().get(key));
        Assert.assertEquals(value_1, vault.logical().read(path, 1).getData().get(key));
        vault.logical().write(path, new HashMap<String, String>() {{
            put(key, value_2);
        }});
        Assert.assertEquals(value_2, vault.logical().read(path, 2).getData().get(key));
        Assert.assertEquals(value_1, vault.logical().read(path, 1).getData().get(key));
        Assert.assertEquals(value_2, vault.logical().read(path).getData().get(key));
        vault.logical().delete(path);
    }
}