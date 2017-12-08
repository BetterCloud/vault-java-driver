package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.AuthRequestValidatingMockVault;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthBackendAwsTests {

    private JsonObject readRequestBody(HttpServletRequest request) {
        try {
            StringBuilder requestBuffer = new StringBuilder();
            IOUtils.readLines(request.getReader()).forEach(requestBuffer::append);
            return Json.parse(requestBuffer.toString()).asObject();
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    public void testLoginByAwsEc2() throws Exception {
        final Predicate<HttpServletRequest> isValidEc2pkcs7Request = (request) -> {
            JsonObject requestBody = readRequestBody(request);
            return requestBody != null && request.getRequestURI().endsWith("/auth/aws/login") &&
                  requestBody.getString("pkcs7", "") == "pkcs7";
        };

        final Predicate<HttpServletRequest> isValidEc2IdRequest = (request) -> {
            JsonObject requestBody = readRequestBody(request);
            return requestBody != null && request.getRequestURI().endsWith("/auth/aws/login") &&
                    requestBody.getString("identity", "") == "identity" &&
                    requestBody.getString("signature", "") == "signature";
        };

        final Predicate<HttpServletRequest> isValidEc2IamRequest = (request) -> {
            JsonObject requestBody = readRequestBody(request);
            return requestBody != null && request.getRequestURI().endsWith("/auth/aws/login") &&
                    requestBody.getString("iam_http_request_method", "") == "POST" &&
                    requestBody.getString("iam_http_request_url", "") == "url" &&
                    requestBody.getString("iam_http_request_body", "") == "body" &&
                    requestBody.getString("iam_http_request_headers", "") == "headers";
        };

        final AuthRequestValidatingMockVault mockVault =  new AuthRequestValidatingMockVault(new HashSet<Predicate<HttpServletRequest>>() {{
            add(isValidEc2pkcs7Request);
            add(isValidEc2IdRequest);
        }});

        final Server server = VaultTestUtils.initHttpMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .build();
        final Vault vault = new Vault(vaultConfig);

        final String token1 = vault.auth()
                .loginByAwsEc2("role","pkcs7",null,null)
                .getAuthClientToken();

        assertNotNull(token1);
        assertEquals("c9368254-3f21-aded-8a6f-7c818e81b17a", token1.trim());

        final String token2 = vault.auth()
                .loginByAwsEc2("role","identity","signature", null, null)
                .getAuthClientToken();

        assertNotNull(token2);
        assertEquals("c9368254-3f21-aded-8a6f-7c818e81b17a", token2.trim());
    }

    @Test
    public void testLoginByAwsIam() throws Exception {
        final Predicate<HttpServletRequest> isValidEc2IamRequest = (request) -> {
            JsonObject requestBody = readRequestBody(request);
            return requestBody != null && request.getRequestURI().endsWith("/auth/aws/login") &&
                    requestBody.getString("iam_http_request_method", "") == "POST" &&
                    requestBody.getString("iam_http_request_url", "") == "url" &&
                    requestBody.getString("iam_http_request_body", "") == "body" &&
                    requestBody.getString("iam_http_request_headers", "") == "headers";
        };

        final AuthRequestValidatingMockVault mockVault =  new AuthRequestValidatingMockVault(new HashSet<Predicate<HttpServletRequest>>() {{
            add(isValidEc2IamRequest);
        }});

        final Server server = VaultTestUtils.initHttpMockVault(mockVault);
        server.start();

        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .build();
        final Vault vault = new Vault(vaultConfig);

        final String token = vault.auth()
                .loginByAwsIam("role","url","body","headers",null)
                .getAuthClientToken();

        assertNotNull(token);
        assertEquals("c9368254-3f21-aded-8a6f-7c818e81b17a", token.trim());
    }
}
