package com.bettercloud.vault;

import org.junit.Assert;
import org.junit.Test;

public class VaultExceptionTests {

    @Test
    public void vaultExceptionMessageTest() {
        VaultException vaultException = new VaultException("VaultException");
        Assert.assertNotNull(vaultException);
        Assert.assertEquals(vaultException.getMessage(), "VaultException");
    }

    @Test
    public void vaultExceptionHttpCodeTest() {
        VaultException vaultException = new VaultException("VaultException", 403);
        Assert.assertNotNull(vaultException);
        Assert.assertEquals(vaultException.getHttpStatusCode(), 403);
    }

    @Test
    public void vaultThrowableTest() {
        Throwable throwable = new Throwable();
        VaultException vaultException = new VaultException(throwable);
        Assert.assertNotNull(vaultException);
        Assert.assertEquals(vaultException.getCause(), throwable);
    }

    @Test
    public void vaultThrowableWithStatusCodeTest() {
        Throwable throwable = new Throwable();
        VaultException vaultException = new VaultException(throwable, 403);
        Assert.assertNotNull(vaultException);
        Assert.assertEquals(vaultException.getCause(), throwable);
        Assert.assertEquals(vaultException.getHttpStatusCode(), 403);
    }
}
