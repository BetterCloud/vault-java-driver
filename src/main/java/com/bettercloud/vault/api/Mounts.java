package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestException;
import com.bettercloud.vault.rest.RestResponse;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>The implementing class for operations on Vault's <code>/v1/sys/mounts/*</code> REST endpoints.</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for usage examples.</p>
 *
 * @see Vault#mounts()
 */
public class Mounts {

    private final VaultConfig config;

    public Mounts(final VaultConfig config) {
        this.config = config;
    }

    /**
     * A container for the list mounts operations response.
     *
     */
    public static class ListMountsResponse {
        List<Mount> data = new LinkedList<>();

        ListMountsResponse(RestResponse restResponse, int retryCount) {
            try {
                final String jsonString = new String(restResponse.getBody(), "UTF-8");
                final JsonObject jsonObject = Json.parse(jsonString).asObject();
                for (String name : jsonObject.names()) {
                    data.add(new Mount(name, jsonObject.get(name).asObject()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        public static class Mount {
            @Getter private final String name;
            @Getter private final String type;
            @Getter private final String description;
            @Getter private final MountConfig config;

            Mount(String name, JsonObject obj) {
                this.name = name;
                this.type = obj.get("type").asString();
                this.description = obj.get("description").asString();
                this.config = new MountConfig(obj.get("config").asObject());
            }

            public static class MountConfig {
                @Getter private final long defaultLeaseTtl;
                @Getter private final long maxLeaseTtl;
                @Getter private final boolean forceNoCache;

                MountConfig(JsonObject obj) {
                    defaultLeaseTtl = obj.get("default_lease_ttl").asLong();
                    maxLeaseTtl = obj.get("max_lease_ttl").asLong();
                    forceNoCache = obj.get("force_no_cache").asBoolean();
                }

            }

        }

    }

    /**
     * List all the mounts available in vault.
     *
     * @return the available mounts
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public ListMountsResponse list() throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/sys/mounts")
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslVerification(config.getSslConfig().isVerify())
                        .sslContext(config.getSslConfig().getSslContext())
                        .get();
                // Validate response
                if (restResponse.getStatus() != 200) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus()
                            + "\nResponse body: " + new String(restResponse.getBody(), "UTF-8"), restResponse.getStatus());
                }

                return new ListMountsResponse(restResponse, retryCount);
            } catch (RuntimeException | VaultException | RestException | UnsupportedEncodingException e) {
                // If there are retries to perform, then pause for the configured interval and then execute the loop again...
                if (retryCount < config.getMaxRetries()) {
                    retryCount++;
                    try {
                        final int retryIntervalMilliseconds = config.getRetryIntervalMilliseconds();
                        Thread.sleep(retryIntervalMilliseconds);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } else if (e instanceof VaultException) {
                    // ... otherwise, give up.
                    throw (VaultException) e;
                } else {
                    throw new VaultException(e);
                }
            }
        }
    }
}

