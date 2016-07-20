package com.bettercloud.vault;

public class VaultException extends Exception {
    private static final long serialVersionUID = 1757161544243811489L;

    public VaultException(final String message) {
        super(message);
    }

    public VaultException(final Throwable t) {
        super(t);
    }

}
