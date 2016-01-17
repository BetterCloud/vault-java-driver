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
        final Response response = new Rest().url("https://httpbin.org/get").type(RequestType.GET).execute();
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


}
