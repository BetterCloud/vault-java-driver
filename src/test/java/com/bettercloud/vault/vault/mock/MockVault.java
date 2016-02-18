package com.bettercloud.vault.vault.mock;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>This class is used to mock out a Vault server in unit tests involving retry logic. As it extends Jetty's
 * <code>AbstractHandler</code>, it can be passed to an embedded Jetty server and respond to actual (albeit
 * localhost) HTTP requests.</p>
 *
 * <p>This basic version simply responds to requests with a pre-determined HTTP status code and response body.
 * Example usage:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final Server server = new Server(8999);
 * server.setHandler( new MockVault(200, "{\"data\":{\"value\":\"mock\"}}") );
 * server.start();
 *
 * final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "mock_token");
 * final Vault vault = new Vault(vaultConfig);
 * final LogicalResponse response = vault.logical().read("secret/hello");
 *
 * assertEquals(200, response.getRestResponse().getStatus());
 * assertEquals("mock", response.getData().get("value"));
 *
 * VaultTestUtils.shutdownMockVault(server);
 * }</pre>
 * </blockquote>
 */
public class MockVault extends AbstractHandler {

    private int mockStatus;
    private String mockResponse;

    public MockVault(final int mockStatus, final String mockResponse) {
        this.mockStatus = mockStatus;
        this.mockResponse = mockResponse;
    }

    protected MockVault() {
    }

    @Override
    public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
    ) throws IOException, ServletException {
        response.setContentType("application/json");
        baseRequest.setHandled(true);
        System.out.println("MockVault is sending an HTTP " + mockStatus + " code, with expected success payload...");
        response.setStatus(mockStatus);
        if (mockResponse != null) {
            response.getWriter().println(mockResponse);
        }
    }

}
