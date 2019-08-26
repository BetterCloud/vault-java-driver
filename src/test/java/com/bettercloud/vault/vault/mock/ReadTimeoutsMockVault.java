package com.bettercloud.vault.vault.mock;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

/**
 * <p>This class is used to mock out a Vault server in unit tests involving read timeouts (i.e. delays between an
 * HTTP(S) connection being successfully established, and downloading of all data being complete).  As it extends
 * Jetty's <code>AbstractHandler</code>, it can be passed to an embedded Jetty server and respond to actual (albeit
 * localhost) HTTP requests.</p>
 *
 * <p>The basic usage pattern is as follows:</p>
 *
 * <ol>
 *     <li>
 *         <code>ReadTimeoutsMockVault</code> accepts the incoming HTTP(S) request.
 *     </li>
 *     <li>
 *         <code>ReadTimeoutsMockVault</code> then pauses for the designated number of seconds.
 *     </li>
 *     <li>
 *         After the delay, <code>ReadTimeoutsMockVault</code> responds with a designated HTTP status code, and
 *         a designated response body.
 *     </li>
 * </ol>
 *
 * <p>Example usage:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final Server server = new Server(8999);
 * server.setHandler( new ReadTimeoutsMockVault(2, 200, "{\"data\":{\"value\":\"mock\"}}") );
 * server.start();
 *
 * final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "mock_token");
 * final VaultConfig vaultConfig = new VaultConfig()
 *                                  .address("http://127.0.0.1:8999")
 *                                  .token("mock_token")
 *                                  .readTimeout(1)
 *                                  .build();
 * try {
 *      final LogicalResponse response = vault.logical().read("secret/hello");
 * } catch (Exception e) {
 *     // An exception is thrown due to the timeout threshold being exceeded.  If the VaultConfig instance were given
 *     // a readTimeout value greater than that set for the ReadTimeoutsMockVault instance, then the call would
 *     // succeed instead.
 *     VaultTestUtils.shutdownMockVault(server);
 *     throw e;
 * }
 * }</pre>
 * </blockquote>
 */
public class ReadTimeoutsMockVault extends MockVault {

    private int delaySeconds;
    private int mockStatus;
    private String mockResponse;

    public ReadTimeoutsMockVault(final int delaySeconds, final int mockStatus, final String mockResponse) {
        this.delaySeconds = delaySeconds;
        this.mockStatus = mockStatus;
        this.mockResponse = mockResponse;
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
        try {
            Thread.sleep(delaySeconds * 1000);
            response.setStatus(mockStatus);
            if (mockResponse != null) {
                response.getWriter().println(mockResponse);
            }
        } catch (InterruptedException e) {
            throw new ServletException(e);
        }
    }

}
