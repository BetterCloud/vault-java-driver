package com.bettercloud.vault;

public class VaultException extends Exception {

    public VaultException(final String message) {
        super(message);
    }

    public VaultException(final Throwable t) {
        super(t);
    }

}
