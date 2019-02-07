package com.bettercloud.vault.api.pki;

import org.junit.Assert;
import org.junit.Test;

public class CredentialFormatTests {

    @Test
    public void CredentialEnumTest() {
        Assert.assertEquals(String.valueOf(CredentialFormat.DER), ("der"));
        Assert.assertEquals(String.valueOf(CredentialFormat.PEM), ("pem"));
        Assert.assertEquals(String.valueOf(CredentialFormat.PEM_BUNDLE), ("pem_bundle"));
    }

    @Test
    public void CredentialFromStringTests() {
        Assert.assertNull(CredentialFormat.fromString(null));
        Assert.assertNotNull(CredentialFormat.fromString(CredentialFormat.DER.toString()));
        Assert.assertNotNull(CredentialFormat.fromString(CredentialFormat.PEM.toString()));
        Assert.assertNotNull(CredentialFormat.fromString(CredentialFormat.PEM_BUNDLE.toString()));
    }
}
