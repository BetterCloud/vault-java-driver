package com.bettercloud.vault.rest;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.EchoInputMockVault;
import java.io.UnsupportedEncodingException;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;

public class DeleteTests {

    /**
     * <p>The REST client should refuse to handle any HTTP verb if the base URL has not already been set.</p>
     */
    @Test(expected = RestException.class)
    public void testFailsOnNoUrl() throws RestException {
        new Rest().delete();
    }

    /**
     * <p>Verify a basic DELETE request, with no parameters or headers.</p>
     *
     * <p>Unfortunately, the "httpbin.org" service that we're using for all of the other REST-layer unit
     * tests does not support the DELETE verb.  So we have to use a self-contained Jetty instance for
     * these tests.</p>
     *
     * @throws RestException
     * @throws UnsupportedEncodingException If there's a problem shutting down the Jetty test server
     */
    @Test
    public void testDelete_Plain() throws Exception {
        final EchoInputMockVault echoInputMockVault = new EchoInputMockVault(204);
        final Server server = VaultTestUtils.initHttpMockVault(echoInputMockVault);
        server.start();

        final RestResponse restResponse = new Rest()//NOPMD
                .url("http://127.0.0.1:8999")
                .delete();
        assertEquals(204, restResponse.getStatus());

        VaultTestUtils.shutdownMockVault(server);
    }

    /**
     * <p>Verify a DELETE request that sends query string parameters.</p>
     *
     * @throws Exception
     */
    @Test
    public void testDelete_WithParams() throws Exception {
        final EchoInputMockVault echoInputMockVault = new EchoInputMockVault(204);
        final Server server = VaultTestUtils.initHttpMockVault(echoInputMockVault);
        server.start();

        final String url = "http://127.0.0.1:8999/?param1=value1&param2=value2";
        final RestResponse restResponse = new Rest()//NOPMD
                .url(url)
                .delete();

        assertEquals(204, restResponse.getStatus());
        assertEquals(url, Json.parse(echoInputMockVault.getLastRequestDetails()).asObject().getString("URL", ""));

        VaultTestUtils.shutdownMockVault(server);
    }

    /**
     * <p>Verify a DELETE request that sends header values.</p>
     *
     * @throws Exception
     */
    @Test
    public void testDelete_WithHeaders() throws Exception {
        final EchoInputMockVault echoInputMockVault = new EchoInputMockVault(204);
        final Server server = VaultTestUtils.initHttpMockVault(echoInputMockVault);
        server.start();

        final RestResponse restResponse = new Rest()//NOPMD
                .url("http://127.0.0.1:8999")
                .header("header1", "value1")
                .header("header2", "value2")
                .delete();

        assertEquals(204, restResponse.getStatus());
        final JsonObject headers = Json.parse(echoInputMockVault.getLastRequestDetails()).asObject().get("headers").asObject();
        assertEquals("value1", headers.getString("header1", ""));
        assertEquals("value2", headers.getString("header2", ""));

        VaultTestUtils.shutdownMockVault(server);
    }

    /**
     * <p>Verify a DELETE request that sends header values.</p>
     *
     * @throws Exception
     */
    @Test
    public void testDelete_WithOptionalHeaders() throws Exception {
        final EchoInputMockVault echoInputMockVault = new EchoInputMockVault(204);
        final Server server = VaultTestUtils.initHttpMockVault(echoInputMockVault);
        server.start();

        final RestResponse restResponse = new Rest()//NOPMD
                .url("http://127.0.0.1:8999")
                .header("header1", "value1")
                .header("header2", "value2")
                .header("I am null", null)
                .delete();

        assertEquals(204, restResponse.getStatus());
        final JsonObject headers = Json.parse(echoInputMockVault.getLastRequestDetails()).asObject().get("headers").asObject();
        assertEquals("value1", headers.getString("header1", ""));
        assertEquals("value2", headers.getString("header2", ""));
        assertNull(headers.getString( "I am null", null));

        VaultTestUtils.shutdownMockVault(server);
    }

}
