package com.bettercloud.vault;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class VaultTests {

    @Test
    public void testRetries() throws Exception {
        final Server server = new Server(8999);
        server.setHandler(new MockVault(5));
        server.start();

        final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "...");
        vaultConfig.retryOption(VaultException.class, 5, 1);
        final Vault vault = new Vault(vaultConfig);
        vault.logical().read("secret/hello");
        System.out.println("done");
    }

}

class MockVault extends AbstractHandler {

    private int failureCount;

    public MockVault(final int failureCount) {
        this.failureCount = failureCount;
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
            System.out.printf("Sending 500... failureCount == %d\n", failureCount);
        } else {
            response.setStatus(200);
            response.getWriter().println("{}");
        }
    }
}

