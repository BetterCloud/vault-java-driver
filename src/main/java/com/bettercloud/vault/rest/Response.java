package com.bettercloud.vault.rest;

public class Response {

    private int status;
    private String mimeType;
    private String body;

    public Response(final int status, final String mimeType, final String body) {
        this.status = status;
        this.mimeType = mimeType;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
