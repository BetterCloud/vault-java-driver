package io.github.jopenlibs.vault.rest;

import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.vault.VaultTestUtils;
import io.github.jopenlibs.vault.vault.mock.EchoInputMockVault;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests relating the REST client processing of GET requests.
 */
public class GetTests {

    private Server server;
    private final String URL = "http://127.0.0.1:8999/";

    @Before
    public void startServer() throws Exception {
        EchoInputMockVault echoInputMockVault = new EchoInputMockVault(200);

        this.server = VaultTestUtils.initHttpMockVault(echoInputMockVault);
        this.server.start();
    }

    @After
    public void stopServer() throws Exception {
        this.server.stop();
    }

    /**
     * The REST client should refuse to handle any HTTP verb if the base URL has not already been
     * set.
     */
    @Test(expected = RestException.class)
    public void testFailsOnNoUrl() throws RestException {
        new Rest().get();
    }

    /**
     * Verify a basic GET request, with no parameters or headers.
     */
    @Test
    public void testGet_Plain() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL)
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("http://127.0.0.1:8999/", jsonObject.getString("URL", null));
    }

    /**
     * Verify a GET request that has no query string on the base URL, but does have additional
     * parameters passed.  Those additional parameters should be appended to the base URL as a query
     * string.
     */
    @Test
    public void testGet_InsertParams() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL)
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals(
                "http://127.0.0.1:8999/?apples=oranges&foo=bar&multi+part=this+parameter+has+whitespace+in+its+name+and+value",
                jsonObject.getString("URL", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value",
                args.getString("multi part", null));
    }

    /**
     * <p>Verify a GET request that has both a query string on the base URL, *and* additional
     * parameters passed.  Those additional parameters should be appended to the query string that's
     * already on the base URL.</p>
     *
     * <p>Note that the original query string is unmodified, but the additional parameters are
     * appended in an order that's sorted by their names.</p>
     */
    @Test
    public void testGet_UpdateParams() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL + "?hot=cold")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals(
                "http://127.0.0.1:8999/?hot=cold&apples=oranges&foo=bar&multi+part=this+parameter+has+whitespace+in+its+name+and+value",
                jsonObject.getString("URL", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("cold", args.getString("hot", null));
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value",
                args.getString("multi part", null));
    }

    /**
     * <p>Verify a GET request that passes HTTP headers.</p>
     *
     * <p>Note that even though our header names are all lowercase, the round-trip process
     * converts them to camel case (e.g. <code>two-part</code> to <code>Two-Part</code>).</p>
     */
    @Test
    public void testGet_WithHeaders() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL)
                .header("Black", "white")
                .header("Day", "night")
                .header("Two-Part", "Header value")
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("http://127.0.0.1:8999/", jsonObject.getString("URL", null));

        final JsonObject headers = jsonObject.get("headers").asObject();
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header value", headers.getString("Two-Part", null));
    }

    /**
     * <p>Verify a GET request that passes HTTP headers.</p>
     *
     * <p>Note that even though our header names are all lowercase, the round-trip process
     * converts them to camel case (e.g. <code>two-part</code> to <code>Two-Part</code>).</p>
     */
    @Test
    public void testGet_WithOptionalHeaders() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL)
                .header("Black", "white")
                .header("Day", "night")
                .header("Two-Part", "Header value")
                .header("I am null", null)
                .get();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals("http://127.0.0.1:8999/", jsonObject.getString("URL", null));

        final JsonObject headers = jsonObject.get("headers").asObject();
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header value", headers.getString("Two-Part", null));
        assertNull(headers.getString("I am null", null));
    }

    /**
     * <p>Verify that response body is retrieved when http status is error code</p>
     */
    @Test
    public void testGet_RetrievesResponseBodyWhenStatusIs418() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL + "status/200")
                .get();
        assertEquals(200, restResponse.getStatus());

        final String responseBody = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        assertTrue("Response body is empty", responseBody.length() > 0);
        assertTrue("Response body doesn't contain word User-Agent",
                responseBody.contains("User-Agent"));
    }

}
