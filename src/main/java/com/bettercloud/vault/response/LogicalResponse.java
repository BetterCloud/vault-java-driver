package com.bettercloud.vault.response;

public class LogicalResponse extends VaultResponse {

    /**
     * TODO: This is just a temporary placeholder.  We need to store all fields found in the "data" object within the response.
     */
    private String value;

    public LogicalResponse(final String value, final int retries) {
        this.value = value;
        this.retries = retries;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

}
