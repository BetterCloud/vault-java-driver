package com.bettercloud.vault.api.pki;

import org.junit.Assert;
import org.junit.Test;

public class PrivateKeyFormatTests {

    @Test
    public void CredentialEnumTest() {
        Assert.assertEquals(String.valueOf(PrivateKeyFormat.DER), ("der"));
        Assert.assertEquals(String.valueOf(PrivateKeyFormat.PEM), ("pem"));
        Assert.assertEquals(String.valueOf(PrivateKeyFormat.PKCS8), ("pkcs8"));
    }

    @Test
    public void CredentialFromStringTests() {
        Assert.assertNull(PrivateKeyFormat.fromString(null));
        Assert.assertNotNull(PrivateKeyFormat.fromString(PrivateKeyFormat.DER.toString()));
        Assert.assertNotNull(PrivateKeyFormat.fromString(PrivateKeyFormat.PEM.toString()));
        Assert.assertNotNull(PrivateKeyFormat.fromString(PrivateKeyFormat.PKCS8.toString()));
    }
}
