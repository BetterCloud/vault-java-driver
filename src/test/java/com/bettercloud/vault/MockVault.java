package com.bettercloud.vault;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO: Document...
 */
public class MockVault extends AbstractHandler {

    private int failureCount ;
    private int successStatus;
    private String successResponse;

    public MockVault(final int failureCount, final int successStatus, final String successResponse) {
        this.failureCount = failureCount;
        this.successStatus = successStatus;
        this.successResponse = successResponse;
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
        if (failureCount > 0) {
            failureCount = failureCount - 1;
            response.setStatus(500);
            System.out.println("Mock Vault is sending an HTTP 500 code, to cause a retry...");
        } else {
            System.out.println("Mock Vault is sending an HTTP 200 code, with expected success payload...");
            response.setStatus(successStatus);
            if (successResponse != null) {
                response.getWriter().println(successResponse);
            }
        }
    }
}
