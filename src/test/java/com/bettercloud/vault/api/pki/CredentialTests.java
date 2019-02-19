package com.bettercloud.vault.api.pki;

import org.junit.Assert;
import org.junit.Test;

public class CredentialTests {

    @Test
    public void credentialTests() {
        Credential credential = new Credential();
        Assert.assertNotNull(credential);
        credential.certificate("certificate");
        credential.issuingCa("issuingCa");
        credential.privateKey("privateKey");
        credential.serialNumber("serialNumber");
        credential.privateKeyType("privateKeyType");
        Assert.assertEquals(credential.getCertificate(),"certificate");
        Assert.assertEquals(credential.getIssuingCa(),"issuingCa");
        Assert.assertEquals(credential.getPrivateKey(),"privateKey");
        Assert.assertEquals(credential.getSerialNumber(),"serialNumber");
        Assert.assertEquals(credential.getPrivateKeyType(),"privateKeyType");
    }
}
