package com.bettercloud.vault.rest;

import java.io.Serializable;
import java.util.Arrays;

/**
 * This class contains the metadata and data that was downloaded by <code>Rest</code>
 * from an HTTP response.
 */
public class RestResponse implements Serializable {

    private int status;
    private String mimeType;
    private byte[] body;

    /**
     *
     * @param status The HTTP status code issues for the response (e.g. <code>200 == OK</code>).
     * @param mimeType The MIME type for the body contents (e.g. <code>application/json</code>).
     * @param body The binary payload of the response body.
     */
    public RestResponse(final int status, final String mimeType, final byte[] body) {
        this.status = status;
        this.mimeType = mimeType;
        this.body = body == null ? null : Arrays.copyOf(body, body.length);
    }

    /**
     * @return The HTTP status code issues for the response (e.g. <code>200 == OK</code>).
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return The MIME type for the body contents (e.g. <code>application/json</code>).
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @return The binary payload of the response body.
     */
    public byte[] getBody() {
        return Arrays.copyOf(body, body.length);
    }

}
