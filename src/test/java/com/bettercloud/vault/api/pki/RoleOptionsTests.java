package com.bettercloud.vault.api.pki;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;

public class RoleOptionsTests {

    @Test
    public void RoleOptionsTests() {
        RoleOptions roleOptions = new RoleOptions();
        Assert.assertNotNull(roleOptions);

        Assert.assertNull(roleOptions.getAllowedDomains());

        roleOptions.allowAnyName(true);
        roleOptions.allowBareDomains(true);
        roleOptions.allowedDomains(new ArrayList<>());
        roleOptions.allowIpSans(true);
        roleOptions.allowLocalhost(true);
        roleOptions.allowSpiffeName(true);
        roleOptions.allowSubdomains(true);
        roleOptions.clientFlag(true);
        roleOptions.codeSigningFlag(true);
        roleOptions.emailProtectionFlag(true);
        roleOptions.enforceHostnames(true);
        roleOptions.ttl("ttl");
        roleOptions.maxTtl("maxTtl");
        roleOptions.keyUsage(new ArrayList<>());
        roleOptions.keyBits(1L);
        roleOptions.useCsrSans(true);
        roleOptions.useCsrCommonName(true);
        roleOptions.keyType("keyType");
        roleOptions.serverFlag(true);

        Assert.assertEquals(roleOptions.getAllowAnyName(), true);
        Assert.assertEquals(roleOptions.getAllowBareDomains(), true);
        Assert.assertEquals(roleOptions.getAllowedDomains(), new ArrayList<>());
        Assert.assertEquals(roleOptions.getAllowIpSans(), true);
        Assert.assertEquals(roleOptions.getAllowLocalhost(), true);
        Assert.assertEquals(roleOptions.getAllowSpiffename(), true);
        Assert.assertEquals(roleOptions.getAllowSubdomains(), true);
        Assert.assertEquals(roleOptions.getClientFlag(), true);
        Assert.assertEquals(roleOptions.getCodeSigningFlag(), true);
        Assert.assertEquals(roleOptions.getEmailProtectionFlag(), true);
        Assert.assertEquals(roleOptions.getEnforceHostnames(), true);
        Assert.assertEquals(roleOptions.getTtl(), "ttl");
        Assert.assertEquals(roleOptions.getMaxTtl(), "maxTtl");
        Assert.assertEquals(roleOptions.getKeyUsage(), new ArrayList<>());
        Assert.assertEquals(roleOptions.getKeyBits().toString(), String.valueOf(1L));
        Assert.assertEquals(roleOptions.getUseCsrSans(), true);
        Assert.assertEquals(roleOptions.getUseCsrCommonName(), true);
        Assert.assertEquals(roleOptions.getKeyType(), "keyType");
        Assert.assertEquals(roleOptions.getServerFlag(), true);
    }
}
