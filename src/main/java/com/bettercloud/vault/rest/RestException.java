package com.bettercloud.vault.rest;

public class RestException extends Exception {

    public RestException(final String message) {
        super(message);
    }

    public RestException(final Throwable t) {
        super(t);
    }

}
