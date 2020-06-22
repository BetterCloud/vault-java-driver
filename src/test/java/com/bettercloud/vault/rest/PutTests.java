package com.bettercloud.vault.rest;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests relating the REST client processing of PUT requests.
 */
public class PutTests {

    /**
     * Verify a basic PUT request, with no parameters or headers.
     *
     * @throws RestException
     */
    @Test
    public void testPut_Plain() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/put")
                .put();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/put", jsonObject.getString("url", null));
    }

    /**
     * Verify a PUT request that has no query string on the base URL, but does have additional
     * parameters passed.  The base URL should remain unmodified, and the parameters should be
     * sent with the request body.
     *
     * @throws RestException
     */
    @Test
    public void testPut_InsertParams() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/put")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .put();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/put", jsonObject.getString("url", null));

        // Note that with a PUT (as with a POST) to this "httpbin.org" test service, parameters are
        // returned within a JSON object called "form", unlike it's "args" counterpart when doing a GET.
        final JsonObject form = jsonObject.get("form").asObject();
        assertEquals("bar", form.getString("foo", null));
        assertEquals("oranges", form.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value", form.getString("multi part", null));
    }

    /**
     * Verify a PUT request that already has a query string on the base URL, but also has additional
     * parameters passed.  The base URL should remain unmodified, and the parameters should be
     * sent with the request body.
     *
     * @throws RestException
     */
    @Test
    public void testPut_UpdateParams() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/put?hot=cold")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .put();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/put?hot=cold", jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("cold", args.getString("hot", null));
        final JsonObject form = jsonObject.get("form").asObject();
        assertEquals("bar", form.getString("foo", null));
        assertEquals("oranges", form.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value", form.getString("multi part", null));
    }

    /**
     * <p>Verify a PUT request that passes HTTP headers.</p>
     *
     * <p>Note that even though our header names are all lowercase, the round-trip process
     * converts them to camel case (e.g. <code>two-part</code> to <code>Two-Part</code>).</p>
     *
     * @throws RestException
     */
    @Test
    public void testPut_WithHeaders() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/put")
                .header("black", "white")
                .header("day", "night")
                .header("two-part", "Header value")
                .put();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/put", jsonObject.getString("url", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header value", headers.getString("Two-Part", null));
    }

    /**
     * <p>Verify a PUT request that passes optional HTTP headers.</p>
     *
     * <p>Note that even though our header names are all lowercase, the round-trip process
     * converts them to camel case (e.g. <code>two-part</code> to <code>Two-Part</code>).</p>
     *
     * @throws RestException
     */
    @Test
    public void testPut_WithOptionalHeaders() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/put")
                .header("black", "white")
                .header("day", "night")
                .header("two-part", "Header value")
                .header("I am null", null)
                .header("I am empty", "")
                .put();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/put", jsonObject.getString("url", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header value", headers.getString("Two-Part", null));
        assertNull(headers.getString("I am null", null));
        assertNull(headers.getString("I am empty", null));
    }

}
