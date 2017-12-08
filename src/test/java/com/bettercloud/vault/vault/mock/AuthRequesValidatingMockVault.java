package com.bettercloud.vault.vault.mock;

import org.eclipse.jetty.server.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.function.Predicate;

public class AuthRequesValidatingMockVault extends MockVault {
    private Set<Predicate<HttpServletRequest>> validators;

    private final String validResponse = "{\n" +
            "  \"auth\": {\n" +
            "    \"renewable\": true,\n" +
            "    \"lease_duration\": 1800000,\n" +
            "    \"metadata\": {\n" +
            "      \"role_tag_max_ttl\": \"0\",\n" +
            "      \"instance_id\": \"i-de0f1344\",\n" +
            "      \"ami_id\": \"ami-fce36983\",\n" +
            "      \"role\": \"dev-role\",\n" +
            "      \"auth_type\": \"ec2\"\n" +
            "    },\n" +
            "    \"policies\": [\n" +
            "      \"default\",\n" +
            "      \"dev\"\n" +
            "    ],\n" +
            "    \"accessor\": \"20b89871-e6f2-1160-fb29-31c2f6d4645e\",\n" +
            "    \"client_token\": \"c9368254-3f21-aded-8a6f-7c818e81b17a\"\n" +
            "  }\n" +
            "}";


    public AuthRequesValidatingMockVault(Set<Predicate<HttpServletRequest>> validators) {
        this.validators = validators;
    }

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");
        baseRequest.setHandled(true);
        if(validators.stream().anyMatch(p -> p.test(request))) {
            response.setStatus(200);
            response.getWriter().println(validResponse);
        } else {
            response.setStatus(400);
            response.getWriter().println("");
        }
    }
}
