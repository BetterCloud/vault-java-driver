package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by flozano on 25/07/2017.
 */
public class MountsTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
    }


    /**
     * Write a secret and verify that it can be read.
     *
     * @throws VaultException
     */
    @Test
    public void testListMountPoints() throws VaultException {
        final Vault vault = container.getRootVault();
        Mounts.ListMountsResponse result = vault.mounts().list();
    }
}
