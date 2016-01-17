package com.bettercloud.vault.rest;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RestTests {

    @Test(expected = RestException.class)
    public void testFailsOnNoUrl() throws RestException {
        new Rest().execute();
    }

    @Test
    public void testGet_Plain() throws RestException {
        // Request type should default to `RequestType.GET` if not set.  This URL only accepts GET requests, so
        // the request should fail if this default doesn't happen.
        final Response response = new Rest().url("https://httpbin.org/get").execute();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/get", jsonObject.getString("url", null));
    }

    @Test
    public void testGet_InsertParams() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/get")
                .type(RequestType.GET)
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .execute();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/get?foo=bar&apples=oranges", jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
    }

    @Test
    public void testGet_UpdateParams() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/get?hot=cold")
                .type(RequestType.GET)
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .execute();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/get?hot=cold&foo=bar&apples=oranges", jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("cold", args.getString("hot", null));
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
    }

    @Test
    public void testGet_WithHeaders() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/get")
                .type(RequestType.GET)
                .header("black", "white")
                .header("day", "night")
                .header("two-part", "Header names can't have spaces, but values can")
                .execute();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/get", jsonObject.getString("url", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        // Note that even though our header names where all-lowercase, the round trip process converts them to
        // camel case.  Header values should remain unmodified.
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header names can't have spaces, but values can", headers.getString("Two-Part", null));
    }


}
