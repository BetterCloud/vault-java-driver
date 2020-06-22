package com.bettercloud.vault.rest;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests relating the REST client processing of GET requests.
 */
public class GetTests {

    /**
     * The REST client should refuse to handle any HTTP verb if the base URL has not
     * already been set.
     */
    @Test(expected = RestException.class)
    public void testFailsOnNoUrl() throws RestException {
        new Rest().get();
    }

    /**
     * Verify a basic GET request, with no parameters or headers.
     *
     * @throws RestException
     */
    @Test
    public void testGet_Plain() throws RestException {
        final RestResponse restResponse = new Rest().url("https://httpbin.org/get").get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/get", jsonObject.getString("url", null));
    }

    /**
     * Verify a GET request that has no query string on the base URL, but does have additional
     * parameters passed.  Those additional parameters should be appended to the base URL as
     * a query string.
     *
     * @throws RestException
     */
    @Test
    public void testGet_InsertParams() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/get")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/get?apples=oranges&foo=bar&multi+part=this+parameter+has+whitespace+in+its+name+and+value",
                jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value", args.getString("multi part", null));
    }

    /**
     * <p>Verify a GET request that has both a query string on the base URL, *and* additional
     * parameters passed.  Those additional parameters should be appended to the query string
     * that's already on the base URL.</p>
     *
     * <p>Note that the original query string is unmodified, but the additional parameters are
     * appended in an order that's sorted by their names.</p>
     *
     * @throws RestException
     */
    @Test
    public void testGet_UpdateParams() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/get?hot=cold")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/get?hot=cold&apples=oranges&foo=bar&multi+part=this+parameter+has+whitespace+in+its+name+and+value",
                jsonObject.getString("url", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("cold", args.getString("hot", null));
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value", args.getString("multi part", null));
    }

    /**
     * <p>Verify a GET request that passes HTTP headers.</p>
     *
     * <p>Note that even though our header names are all lowercase, the round-trip process
     * converts them to camel case (e.g. <code>two-part</code> to <code>Two-Part</code>).</p>
     *
     * @throws RestException
     */
    @Test
    public void testGet_WithHeaders() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/get")
                .header("black", "white")
                .header("day", "night")
                .header("two-part", "Header value")
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/get", jsonObject.getString("url", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        // Note that even though our header names where all-lowercase, the round trip process converts them to camel case.
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header value", headers.getString("Two-Part", null));
    }

    /**
     * <p>Verify a GET request that passes HTTP headers.</p>
     *
     * <p>Note that even though our header names are all lowercase, the round-trip process
     * converts them to camel case (e.g. <code>two-part</code> to <code>Two-Part</code>).</p>
     *
     * @throws RestException
     */
    @Test
    public void testGet_WithOptionalHeaders() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("https://httpbin.org/get")
                .header("black", "white")
                .header("day", "night")
                .header("two-part", "Header value")
                .header("I am null", null)
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("https://httpbin.org/get", jsonObject.getString("url", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        // Note that even though our header names where all-lowercase, the round trip process converts them to camel case.
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header value", headers.getString("Two-Part", null));
        assertNull(headers.getString("I am null", null));
    }

    /**
     * <p>Verify that response body is retrieved when http status is error code</p>
     *
     * @throws RestException
     */
    @Test
    public void testGet_RetrievesResponseBodyWhenStatusIs418() throws RestException {
        final RestResponse restResponse = new Rest()
                .url("http://httpbin.org/status/418")
                .get();
        assertEquals(418, restResponse.getStatus());

        final String responseBody = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        assertTrue("Response body is empty", responseBody.length() > 0);
        assertTrue("Response body doesn't contain word teapot", responseBody.contains("teapot"));
    }

}
