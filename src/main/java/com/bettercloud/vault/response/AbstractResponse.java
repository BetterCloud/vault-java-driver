package com.bettercloud.vault.response;

public abstract class AbstractResponse {

    protected int retries;

    public int getRetries() {
        return retries;
    }

    public void setRetries(final int retries) {
        this.retries = retries;
    }

}
