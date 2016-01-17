package com.bettercloud.vault.rest;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PostTests {

    @Test
    public void testPost_Plain() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/post")
                .post();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/post", jsonObject.getString("url", null));
    }

    @Test
    public void testPost_InsertParams() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/post")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .post();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        // Note that with a POST, the parameters are written with the request body... and URL is *not* re-written to
        // insert them as a query string.
        assertEquals("https://httpbin.org/post", jsonObject.getString("url", null));

        // Note that with a POST (to this "httpbin.org" test service), parameters are returned within a JSON object
        // called "form", unlike it's "args" counterpart when doing a GET.
        final JsonObject form = jsonObject.get("form").asObject();
        assertEquals("bar", form.getString("foo", null));
        assertEquals("oranges", form.getString("apples", null));
    }

    @Test
    public void testPost_UpdateParams() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/post?hot=cold")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .post();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/post?hot=cold", jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("cold", args.getString("hot", null));
        final JsonObject form = jsonObject.get("form").asObject();
        assertEquals("bar", form.getString("foo", null));
        assertEquals("oranges", form.getString("apples", null));
    }

    @Test
    public void testPost_WithHeaders() throws RestException {
        final Response response = new Rest()
                .url("https://httpbin.org/post")
                .header("black", "white")
                .header("day", "night")
                .header("two-part", "Header names can't have spaces, but values can")
                .post();
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMimeType());

        final JsonObject jsonObject = Json.parse(response.getBody()).asObject();
        assertEquals("https://httpbin.org/post", jsonObject.getString("url", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header names can't have spaces, but values can", headers.getString("Two-Part", null));
    }

}
