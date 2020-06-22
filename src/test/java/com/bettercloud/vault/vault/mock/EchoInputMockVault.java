package com.bettercloud.vault.vault.mock;

import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

/**
 * <p>This class is used for inspecting the request inputs to ensure they were properly
 * received, for use cases where the server doesn't really return much helpful
 * information.  Currently, this only includes testing DELETE calls in the low-level
 * REST library code.</p>
 *
 * <p>The <code>handle()</code> method simply parses detail about the request into
 * a JSON string, and returns with the response body.  Currently, we're finding that
 * this body is not being received by the requestor... which is probably a quirk of
 * the HTTP spec that needs further investigation.  For now, we're also populating
 * the details of the most recent request a member variable, so that it can be retrieved
 * that way by unit tests.</p>
 */
public class EchoInputMockVault extends MockVault {

    private int mockStatus;
    private String lastRequestDetails;

    public EchoInputMockVault(final int mockStatus) {
        this.mockStatus = mockStatus;
    }

    @Override
    public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
    ) throws IOException {
        final JsonObject headers = Json.object();
        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String name = headerNames.nextElement();
            final String value = request.getHeader(name);
            headers.add(name, value);
        }

        final StringBuilder url = new StringBuilder(request.getScheme())
                .append("://")
                .append(request.getServerName())
                .append(request.getServerPort() == 0 ? "" : ":" + request.getServerPort())
                .append(request.getRequestURI())
                .append(request.getQueryString() == null || request.getQueryString().isEmpty() ? "" : "?" +
                        request.getQueryString());
        final String mockResponse = Json.object()
                .add("method", request.getMethod())
                .add("URL", url.toString())
                .add("headers", headers)
                .toString();

        response.setContentType("application/json");
        baseRequest.setHandled(true);
        response.setStatus(mockStatus);
        response.getWriter().println(mockResponse);
        this.lastRequestDetails = mockResponse;
    }

    public String getLastRequestDetails() {
        return lastRequestDetails;
    }
}
