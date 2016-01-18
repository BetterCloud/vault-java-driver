package com.bettercloud.vault.rest;

public class Response {

    private int status;
    private String mimeType;
    private byte[] body;

    public Response(final int status, final String mimeType, final byte[] body) {
        this.status = status;
        this.mimeType = mimeType;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

}
