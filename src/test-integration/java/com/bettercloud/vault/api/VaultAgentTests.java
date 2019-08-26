package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.util.VaultAgentContainer;
import com.bettercloud.vault.util.VaultContainer;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.assertNotNull;

public class VaultAgentTests {
    @ClassRule
    public static final VaultContainer container = new VaultContainer();
    @ClassRule
    public static final TemporaryFolder temp = new TemporaryFolder();
    @ClassRule
    public static VaultAgentContainer vaultAgentContainer;

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException, VaultException {
        container.initAndUnsealVault();
        container.setupBackendAppRole();
        container.setEngineVersions();

        final Vault vault = container.getRootVaultWithCustomVaultConfig(new VaultConfig().engineVersion(1));

        final LogicalResponse roleIdResponse = vault.logical().read("auth/approle/role/testrole/role-id");
        String appRoleId = roleIdResponse.getData().get("role_id");
        final LogicalResponse secretIdResponse = vault.logical().write("auth/approle/role/testrole/secret-id", null);
        String secretId = secretIdResponse.getData().get("secret_id");

        assertNotNull(appRoleId);
        assertNotNull(secretId);

        temp.create();
        File role_id = temp.newFile("role_id");
        File secret_id = temp.newFile("secret_id");
        writeStringToFile(role_id, appRoleId);
        writeStringToFile(secret_id, secretId);
        vaultAgentContainer = new VaultAgentContainer(role_id.toPath(), secret_id.toPath());
        vaultAgentContainer.start();
    }

    @Test
    public void testWriteAndReadFromAgent() throws VaultException {
        final String pathToWrite = "secret/hello";
        final String pathToRead = "secret/hello";

        final String value = "world";
        final Vault vault = vaultAgentContainer.getVault();

        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("value", value);

        vault.logical().write(pathToWrite, testMap);

        final String valueRead = vault.logical().read(pathToRead).getData().get("value");
        assertEquals(value, valueRead);
    }
}
