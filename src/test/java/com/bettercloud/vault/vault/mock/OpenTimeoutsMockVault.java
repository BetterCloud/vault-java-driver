package com.bettercloud.vault.vault.mock;

import org.eclipse.jetty.server.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO: Document
 *
 * TODO: Add support for read timeouts
 */
public class OpenTimeoutsMockVault extends MockVault {

    private int delaySeconds;
    private int mockStatus;
    private String mockResponse;

    public OpenTimeoutsMockVault(final int delaySeconds, final int mockStatus, final String mockResponse) {
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
            if (response != null) {
                response.getWriter().println(mockResponse);
            }
        } catch (InterruptedException e) {
            throw new ServletException(e);
        }
    }

}
