package com.bettercloud.vault.api.transit;

public class DecryptOptions {

    private String ciphertext;

    private String context;

    private String nonce;


    public String getCiphertext() {
        return ciphertext;
    }

    public String getContext() {
        return context;
    }


    public String getNonce() {
        return nonce;
    }

    public DecryptOptions ciphertext(String ciphertext) {
        this.ciphertext = ciphertext;
        return this;
    }

    public DecryptOptions context(String context) {
        this.context = context;
        return this;
    }

    public DecryptOptions nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }
}
