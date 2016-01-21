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
        server.setHandler(new MockVault());
        server.start();

        final VaultConfig vaultConfig = new VaultConfig("http://127.0.0.1:8999", "...");
        final Vault vault = new Vault(vaultConfig);
        vault.logical().write("secret/hello", "world");
        System.out.println("done");
    }

}

class MockVault extends AbstractHandler {
    @Override
    public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
    ) throws IOException, ServletException {
        response.setContentType("application/json");
        response.setStatus(200);
        baseRequest.setHandled(true);
        response.getWriter().println("{}");
    }
}
