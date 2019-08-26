package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.api.mounts.Mount;
import com.bettercloud.vault.api.mounts.MountConfig;
import com.bettercloud.vault.api.mounts.MountPayload;
import com.bettercloud.vault.api.mounts.MountType;
import com.bettercloud.vault.api.mounts.TimeToLive;
import com.bettercloud.vault.response.MountResponse;
import com.bettercloud.vault.util.VaultContainer;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/** Integration tests for for operations on Vault's <code>/v1/sys/mounts/*</code> REST endpoints. */
public class MountsTests {

    @ClassRule
    public static final VaultContainer container = new VaultContainer();

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @BeforeClass
    public static void setupClass() throws IOException, InterruptedException {
        container.initAndUnsealVault();
        container.setupBackendPki();
    }

    @Test
    public void testList() throws VaultException {
        final Vault vault = container.getRootVault();

        final MountResponse response = vault.mounts().list();
        final Map<String, Mount> mounts = response.getMounts();

        assertTrue(mounts.containsKey("pki-custom-path-1/"));
        assertTrue(mounts.containsKey("pki-custom-path-2/"));
        assertTrue(mounts.containsKey("pki-custom-path-3/"));
    }

    @Test
    public void testEnable() throws VaultException {
        final Vault vault = container.getRootVault();

        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(12, TimeUnit.HOURS))
                .maxLeaseTtl(TimeToLive.of(12, TimeUnit.HOURS))
                .description("description for pki engine");

        final MountResponse response = vault.mounts().enable("pki-itest-path-1", MountType.PKI, payload);

        assertEquals(204, response.getRestResponse().getStatus());
    }

    @Test
    public void testEnableExceptionAlreadyExist() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("path is already in use");

        final Vault vault = container.getRootVault();

        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(168, TimeUnit.HOURS))
                .maxLeaseTtl(TimeToLive.of(168, TimeUnit.HOURS))
                .description("description for pki engine");

        vault.mounts().enable("pki-custom-path-1", MountType.PKI, payload);
    }

    @Test
    public void testEnableExceptionNullType() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("Mount type is missing");

        final Vault vault = container.getRootVault();

        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(30, TimeUnit.MINUTES))
                .maxLeaseTtl(TimeToLive.of(30, TimeUnit.MINUTES))
                .description("description for pki engine");

        vault.mounts().enable("pki-itest-path-2", null, payload);
    }

    @Test
    public void testEnableExceptionNullTimeUnit() throws VaultException {
        expectedEx.expect(NullPointerException.class);

        final Vault vault = container.getRootVault();

        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(7, null));

        vault.mounts().enable("pki-itest-path-3", MountType.PKI, payload);
    }

    @Test
    public void testEnableExceptionInvalidTimeUnit() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("is not a vaild TimeUnit for Vault");

        final Vault vault = container.getRootVault();

        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(7, TimeUnit.DAYS));

        vault.mounts().enable("pki-itest-path-4", MountType.PKI, payload);
    }

    @Test
    public void testDisable() throws VaultException {
        final Vault vault = container.getRootVault();

        final MountResponse response = vault.mounts().disable("pki-custom-path-3");

        assertEquals(204, response.getRestResponse().getStatus());
    }

    @Test
    public void testRead() throws VaultException {
        final Vault vault = container.getRootVault();

        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(360, TimeUnit.MINUTES))
                .maxLeaseTtl(TimeToLive.of(360, TimeUnit.MINUTES));

        vault.mounts().enable("pki-predefined-path-1", MountType.PKI, payload);

        final MountResponse response = vault.mounts().read("pki-predefined-path-1");
        final Mount mount = response.getMount();
        final MountConfig config = mount.getConfig();

        assertEquals(200, response.getRestResponse().getStatus());

        assertEquals(Integer.valueOf(21600), config.getDefaultLeaseTtl());
        assertEquals(Integer.valueOf(21600), config.getMaxLeaseTtl());
    }

    @Test
    public void testReadExceptionNotFound() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("cannot fetch sysview for path");

        final Vault vault = container.getRootVault();

        vault.mounts().read("pki-non-existing-path");
    }

    @Test
    public void testTune() throws VaultException {
        final Vault vault = container.getRootVault();

        final MountPayload enablePayload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(6, TimeUnit.HOURS))
                .maxLeaseTtl(TimeToLive.of(6, TimeUnit.HOURS));

        vault.mounts().enable("pki-predefined-path-2", MountType.PKI, enablePayload);

        final MountPayload tunePayload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(12, TimeUnit.HOURS))
                .maxLeaseTtl(TimeToLive.of(12, TimeUnit.HOURS));

        final MountResponse tuneResponse = vault.mounts().tune("pki-predefined-path-2", tunePayload);

        assertEquals(204, tuneResponse.getRestResponse().getStatus());

        final MountResponse response = vault.mounts().read("pki-predefined-path-2");
        final Mount mount = response.getMount();
        final MountConfig config = mount.getConfig();

        assertEquals(Integer.valueOf(43200), config.getDefaultLeaseTtl());
        assertEquals(Integer.valueOf(43200), config.getMaxLeaseTtl());
    }

    @Test
    public void testTuneExceptionNotFound() throws VaultException {
        expectedEx.expect(VaultException.class);
        expectedEx.expectMessage("no mount entry found");

        final Vault vault = container.getRootVault();

        final MountPayload tunePayload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(24, TimeUnit.HOURS))
                .maxLeaseTtl(TimeToLive.of(24, TimeUnit.HOURS));

        vault.mounts().tune("pki-non-existing-path", tunePayload);
    }
}
