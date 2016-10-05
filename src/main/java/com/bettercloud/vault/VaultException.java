package com.bettercloud.vault;

public class VaultException extends Exception {

    private int httpStatusCode;

    public VaultException(final String message) {
        super(message);
    }

    public VaultException(final Throwable t) {
        super(t);
    }

    /**
     * Use this constructor to generate a <code>VaultException</code> instance that is based on receiving a
     * particular HTTP status code from a Vault server (e.g. 500).
     *
     * @param message A string expressing the exception cause
     * @param httpStatusCode An HTTP status code returned by a Vault server (e.g. 500)
     */
    public VaultException(final String message, final int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Use this constructor to generate a <code>VaultException</code> instance that is based on receiving a
     * particular HTTP status code from a Vault server (e.g. 500).
     *
     * @param t Another exception that this <code>VaultException</code> will wrap
     * @param httpStatusCode An HTTP status code returned by a Vault server (e.g. 500)
     */
    public VaultException(final Throwable t, final int httpStatusCode) {
        super(t);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * An HTTP status code, returned from a Vault server, that is the cause of this <code>VaultException</code>.
     *
     * Some exceptions are not caused on the Vault side.  Obviously, if there is no relevant HTTP status code then
     * this method will return <code>0</code>.
     *
     * @return An HTTP status code, returned from a Vault server, that is the cause of this <code>VaultException</code>
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

}
