package com.bettercloud.vault.rest;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GetTests {

    @Test(expected = RestException.class)
    public void testFailsOnNoUrl() throws RestException {
        new Rest().get();
    }

    @Test
    public void testGet_Plain() throws RestException {
        final Response response = new Rest().url("https://httpbin.org/get").get();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/get", jsonObject.getString("url", null));
    }

    @Test
    public void testGet_InsertParams() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/get")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/get?apples=oranges&foo=bar&multi+part=this+parameter+has+whitespace+in+its+name+and+value",
                jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value", args.getString("multi part", null));
    }

    @Test
    public void testGet_UpdateParams() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/get?hot=cold")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/get?hot=cold&apples=oranges&foo=bar&multi+part=this+parameter+has+whitespace+in+its+name+and+value",
                jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("cold", args.getString("hot", null));
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value", args.getString("multi part", null));
    }

    @Test
    public void testGet_WithHeaders() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/get")
                .header("black", "white")
                .header("day", "night")
                .header("two-part", "Note that headers are send in url-encoded format")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/get", jsonObject.getString("url", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        // Note that even though our header names where all-lowercase, the round trip process converts them to camel case.
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Note+that+headers+are+send+in+url-encoded+format", headers.getString("Two-Part", null));
    }

}
