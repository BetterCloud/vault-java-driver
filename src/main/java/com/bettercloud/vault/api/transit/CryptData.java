package com.bettercloud.vault.api.transit;

public class CryptData {

    private String ciphertext;

    private String plaintext;

    private Integer keyVersion;

    public String getCiphertext() {
        return ciphertext;
    }

    public String getPlaintext() {
        return plaintext;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public CryptData ciphertext(String ciphertext){
        this.ciphertext = ciphertext;
        return this;
    }

    public CryptData plaintext(String plaintext){
        this.plaintext = plaintext;
        return this;
    }

    public CryptData keyVersion(Integer keyVersion){
        this.keyVersion = keyVersion;
        return this;
    }
}
