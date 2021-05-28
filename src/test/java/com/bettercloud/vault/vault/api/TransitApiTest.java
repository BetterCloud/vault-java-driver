package com.bettercloud.vault.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.vault.VaultTestUtils;
import com.bettercloud.vault.vault.mock.MockVault;
import java.util.Collections;
import java.util.Optional;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransitApiTest {

    private static final String[] PLAIN_DATA = {"MU45MjIyMTM=", "MU45MjIxMTM=", "MA=="};

    private static final String[] CIPHER_DATA = {
        "1jFhRYWHiddSKgEFyVRpX8ieX7UU+748NBw",
        "1jFhRYWHiddSKgEFyVRpX8ieX7UU+748NBw",
        "HKecXE3hnGBoAxrfgoD5U0yAvji7b5X6V1fP"
    };

    private Server server;
    private MockVault vaultServer;

    public void start(String response) throws Exception {
        vaultServer = new MockVault(200, response);
        server = VaultTestUtils.initHttpMockVault(vaultServer);
        server.start();
    }

    @After
    public void after() throws Exception {
        VaultTestUtils.shutdownMockVault(server);
    }

    @Test
    public void testTransitEncrypt() throws Exception {
        final JsonObject expectedRequest = new JsonObject()
                .add("plaintext", PLAIN_DATA[0]);
        final JsonObject expectedResponse = new JsonObject()
                .add("data", new JsonObject()
                        .add("ciphertext", CIPHER_DATA[0]));

        start(expectedResponse.toString());

        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .build();
        final Vault vault = new Vault(vaultConfig, 1);

        LogicalResponse response = vault.logical().write("transit/encrypt/test",
                Collections.singletonMap("plaintext", PLAIN_DATA[0]));

        assertEquals("http://127.0.0.1:8999/v1/transit/encrypt/test", vaultServer.getRequestUrl());
        assertEquals(Optional.of(expectedRequest), vaultServer.getRequestBody());
        assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testTransitDecrypt() throws Exception {
        final JsonObject expectedRequest = new JsonObject()
                .add("ciphertext", CIPHER_DATA[0]);
        final JsonObject expectedResponse = new JsonObject()
                .add("data", new JsonObject()
                        .add("plaintext", PLAIN_DATA[0]));

        start(expectedResponse.toString());

        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .build();
        final Vault vault = new Vault(vaultConfig, 1);

        LogicalResponse response = vault.logical().write("transit/decrypt/test",
                Collections.singletonMap("ciphertext", CIPHER_DATA[0]));

        assertEquals("http://127.0.0.1:8999/v1/transit/decrypt/test", vaultServer.getRequestUrl());
        assertEquals(Optional.of(expectedRequest), vaultServer.getRequestBody());
        assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testBulkTransitEncrypt() throws Exception {
        JsonArray batchRequest = new JsonArray();
        for (String text : PLAIN_DATA) {
            batchRequest.add(new JsonObject().add("plaintext", text));
        }
        JsonArray batchResponse = new JsonArray();
        for (String text : CIPHER_DATA) {
            batchResponse.add(new JsonObject().add("ciphertext", text));
        }
        final JsonObject expectedRequest = new JsonObject()
                .add("batch_input", batchRequest);
        final JsonObject expectedResponse = new JsonObject()
                .add("data", new JsonObject()
                        .add("batch_results", batchResponse));

        start(expectedResponse.toString());

        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .build();
        final Vault vault = new Vault(vaultConfig, 1);

        JsonArray batch = new JsonArray();
        for (String text : PLAIN_DATA) {
            batch.add(new JsonObject().add("plaintext", text));
        }
        LogicalResponse response = vault.logical().write("transit/encrypt/test",
                Collections.singletonMap("batch_input", batch));

        assertEquals(Optional.of(expectedRequest), vaultServer.getRequestBody());
        assertEquals("http://127.0.0.1:8999/v1/transit/encrypt/test", vaultServer.getRequestUrl());
        assertEquals(200, response.getRestResponse().getStatus());
    }

    @Test
    public void testBulkTransitDecrypt() throws Exception {
        JsonArray batchRequest = new JsonArray();
        for (String text : CIPHER_DATA) {
            batchRequest.add(new JsonObject().add("ciphertext", text));
        }
        JsonArray batchResponse = new JsonArray();
        for (String text : PLAIN_DATA) {
            batchResponse.add(new JsonObject().add("plaintext", text));
        }
        final JsonObject expectedRequest = new JsonObject()
                .add("batch_input", batchRequest);
        final JsonObject expectedResponse = new JsonObject()
                .add("data", new JsonObject()
                    .add("batch_results", batchResponse));

        start(expectedResponse.toString());

        final VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:8999")
                .build();
        final Vault vault = new Vault(vaultConfig, 1);

        JsonArray batch = new JsonArray();
        for (String text : CIPHER_DATA) {
            batch.add(new JsonObject().add("ciphertext", text));
        }
        LogicalResponse response = vault.logical().write("transit/decrypt/test",
                Collections.singletonMap("batch_input", batch));

        assertEquals(Optional.of(expectedRequest), vaultServer.getRequestBody());
        assertEquals("http://127.0.0.1:8999/v1/transit/decrypt/test", vaultServer.getRequestUrl());
        assertEquals(200, response.getRestResponse().getStatus());
    }

}
