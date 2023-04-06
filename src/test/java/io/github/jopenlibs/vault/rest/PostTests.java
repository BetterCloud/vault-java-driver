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

/**
 * Unit tests relating the REST client processing of POST requests.
 */
public class PostTests {

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
     * Verify a basic POST request, with no parameters or headers.
     */
    @Test
    public void testPost_Plain() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL)
                .post();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals(this.URL, jsonObject.getString("URL", null));
    }

    /**
     * Verify a POST request that has no query string on the base URL, but does have additional
     * parameters passed.  The base URL should remain unmodified, and the parameters should be sent
     * with the request body.
     */
    @Test
    public void testPost_InsertParams() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL)
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .post();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals(this.URL, jsonObject.getString("URL", null));

        final JsonObject form = jsonObject.get("args").asObject();
        assertEquals("bar", form.getString("foo", null));
        assertEquals("oranges", form.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value",
                form.getString("multi part", null));
    }

    /**
     * Verify a POST request that already has a query string on the base URL, but also has
     * additional parameters passed.  The base URL should remain unmodified, and the parameters
     * should be sent with the request body.
     */
    @Test
    public void testPost_UpdateParams() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL + "?hot=cold")
                .parameter("foo", "bar")
                .parameter("apples", "oranges")
                .parameter("multi part", "this parameter has whitespace in its name and value")
                .post();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals(this.URL + "?hot=cold", jsonObject.getString("URL", null));
        final JsonObject args = jsonObject.get("args").asObject();
        assertEquals("cold", args.getString("hot", null));
        assertEquals("bar", args.getString("foo", null));
        assertEquals("oranges", args.getString("apples", null));
        assertEquals("this parameter has whitespace in its name and value",
                args.getString("multi part", null));
    }

    /**
     * <p>Verify a POST request that passes HTTP headers.</p>
     *
     * <p>Note that even though our header names are all lowercase, the round-trip process
     * converts them to camel case (e.g. <code>two-part</code> to <code>Two-Part</code>).</p>
     */
    @Test
    public void testPost_WithHeaders() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL)
                .header("Black", "white")
                .header("Day", "night")
                .header("Two-Part", "Header value")
                .post();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals(this.URL, jsonObject.getString("URL", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header value", headers.getString("Two-Part", null));
    }

    /**
     * <p>Verify a POST request that passes optional HTTP headers.</p>
     *
     * <p>Note that even though our header names are all lowercase, the round-trip process
     * converts them to camel case (e.g. <code>two-part</code> to <code>Two-Part</code>).</p>
     */
    @Test
    public void testPost_WithOptionalHeaders() throws RestException {
        final RestResponse restResponse = new Rest()
                .url(this.URL)
                .header("Black", "white")
                .header("Day", "night")
                .header("Two-Part", "Header value")
                .header("I am null", null)
                .header("I am empty", "")
                .post();
        assertEquals(200, restResponse.getStatus());
        assertEquals("application/json", restResponse.getMimeType());

        final String jsonString = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        assertEquals(this.URL, jsonObject.getString("URL", null));
        final JsonObject headers = jsonObject.get("headers").asObject();
        assertEquals("white", headers.getString("Black", null));
        assertEquals("night", headers.getString("Day", null));
        assertEquals("Header value", headers.getString("Two-Part", null));
        assertNull(headers.getString("I am null", null));
        assertNull(headers.getString("I am empty", null));
    }


}
