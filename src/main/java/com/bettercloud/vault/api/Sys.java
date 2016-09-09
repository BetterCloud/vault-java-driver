package com.bettercloud.vault.api;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.SysResponse;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>A wrapper around the implementing classes for all of Vault's various
 * <code>/v1/sys/*</code> endpoints.  Because there are so many of them (most Vault API operations
 * fall under that root path), this Java API groups them by the categories suggested on
 * the Vault documentation page (https://www.vaultproject.io/docs/http/index.html).</p>
 *
 * <p>To make calls on implementing classes within this wrapper, just go one level deeper
 * that usual in the builder pattern style:</p>
 *
 * <blockquote>
 * <pre>{@code
 * final HealthResponse response = vault.sys().debug().health();
 * }</pre>
 * </blockquote>
 */
public class Sys {

    private final VaultConfig config;

    public Sys(final VaultConfig config) {
        this.config = config;
    }

    /**
     * Returns the implementing class for operations on Vault's <code>/v1/sys/*</code> REST endpoints,
     * under the "Debug" section of the Vault HTTP API docs (https://www.vaultproject.io/docs/http/index.html).
     *
     * @return The implementing class for debugging-related endpoints in the Vault HTTP API.
     */
    public Debug debug() {
        return new Debug(config);
    }

    /**
     * <p>Basic operation to update a status.
     * E.g.:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final SysResponse response = vault.sys().put("sys/revoke-prefix/aws");
     * }</pre>
     * </blockquote>
     *
     * @param path The path of the status which will be updated (e.g. <code>sys/revoke-prefix/aws</code>)
     * @throws VaultException
     */
    public SysResponse put(final String path) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/" + path)
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .put();

                System.out.println(restResponse);
                // Validate response
                final Set<Integer> validCodes = new HashSet<Integer>(Arrays.asList(204, 429, 500));//NOPMD
                if (!validCodes.contains(restResponse.getStatus())) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }
                return new SysResponse(restResponse, retryCount);
            } catch (Exception e) {
                // If there are retries to perform, then pause for the configured interval and then execute the loop again...
                if (retryCount < config.getMaxRetries()) {
                    retryCount++;
                    try {
                        final int retryIntervalMilliseconds = config.getRetryIntervalMilliseconds();
                        Thread.sleep(retryIntervalMilliseconds);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    // ... otherwise, give up.
                    throw new VaultException(e);
                }
            }
        }
    }

}
