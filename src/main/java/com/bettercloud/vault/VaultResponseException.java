package com.bettercloud.vault;

public class VaultResponseException extends VaultException {
    private static final long serialVersionUID = 7622562542614338888L;

    private final int responseCode;

    public VaultResponseException(int responseCode) {
        super("Vault responded with HTTP status code: " + responseCode);

        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
