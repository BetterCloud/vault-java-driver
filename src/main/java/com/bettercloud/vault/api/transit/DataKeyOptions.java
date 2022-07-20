package com.bettercloud.vault.api.transit;

public class DataKeyOptions {

    private String context;


    private String nonce;

    private Integer bits;




    public String getContext() {
        return context;
    }

    public String getNonce() {
        return nonce;
    }

    public Integer getBits() {
        return bits;
    }

    public DataKeyOptions context(String context) {
        this.context = context;
        return this;
    }

    public DataKeyOptions nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public DataKeyOptions bits(int bits) {
        this.bits = bits;
        return this;
    }
}
