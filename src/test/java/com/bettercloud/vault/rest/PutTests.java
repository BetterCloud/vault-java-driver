package com.bettercloud.vault.rest;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PutTests {

    @Test
    public void testPut_Plain() throws RestException, UnsupportedEncodingException {
        final Response response = new Rest()
                .url("https://httpbin.org/put")
                .put();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final String jsonString = new String(response.getBody(), "UTF-8");
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/put", jsonObject.getString("url", null));
    }

    @Test
    public void testPut_InsertParams() throws RestException, UnsupportedEncodingException {
        final Response response = new Rest()
                .url("https://httpbin.org/put")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .put();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final String jsonString = new String(response.getBody(), "UTF-8");
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        // Note that with a PUT (as with a POST), the parameters are written with the request
        // body... and URL is *not* re-written to insert them as a query string.
        assertEquals("https://httpbin.org/put", jsonObject.getString("url", null));

        // Note that with a PUT (as with a POST) to this "httpbin.org" test service, parameters
        // are returned within a JSON object called "form", unlike it's "args" counterpart when
        // doing a GET.
        final JsonObject form = jsonObject.get("form").asObject();
        assertEquals("bar", form.getString("foo", null));
        assertEquals("oranges", form.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value", form.getString("multi part", null));
    }

    @Test
    public void testPut_UpdateParams() throws RestException, UnsupportedEncodingException {
        final Response response = new Rest()
                .url("https://httpbin.org/put?hot=cold")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .put();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final String jsonString = new String(response.getBody(), "UTF-8");
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/put?hot=cold", jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("cold", args.getString("hot", null));
        final JsonObject form = jsonObject.get("form").asObject();
        assertEquals("bar", form.getString("foo", null));
        assertEquals("oranges", form.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value", form.getString("multi part", null));
    }

    @Test
    public void testPut_WithHeaders() throws RestException, UnsupportedEncodingException {
        final Response response = new Rest()
                .url("https://httpbin.org/put")
                .header("black", "white")
                .header("day", "night")
                .header("two-part", "Note that headers are send in url-encoded format")
                .put();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final String jsonString = new String(response.getBody(), "UTF-8");
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/put", jsonObject.getString("url", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Note+that+headers+are+send+in+url-encoded+format", headers.getString("Two-Part", null));
    }

}
