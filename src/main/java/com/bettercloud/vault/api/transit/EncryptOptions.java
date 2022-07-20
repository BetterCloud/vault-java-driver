package com.bettercloud.vault.api.transit;

public class EncryptOptions {

    private byte[] plaintext;

    private String context;

    private Integer keyVersion;

    private String nonce;


    public byte[] getPlaintext() {
        return plaintext;
    }

    public String getContext() {
        return context;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public String getNonce() {
        return nonce;
    }

    public EncryptOptions plaintext(byte[] plaintext) {
        this.plaintext = plaintext;
        return this;
    }

    public EncryptOptions context(String context) {
        this.context = context;
        return this;
    }

    public EncryptOptions keyVersion(int keyVersion) {
        this.keyVersion = keyVersion;
        return this;
    }

    public EncryptOptions nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }
}
