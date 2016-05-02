package com.bettercloud.vault.api.pki;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.Rest;
import com.bettercloud.vault.rest.RestResponse;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Document
 */
public class Pki {

    private final VaultConfig config;

    public Pki(final VaultConfig config) {
        this.config = config;
    }

    /**
     * TODO: Document
     *
     * TODO: Create return type
     *
     * TODO: Change RoleOptions csv fields to Lists
     *
     * @param roleName
     * @param options
     */
    public LogicalResponse createOrUpdateRole(final String roleName, final RoleOptions options) throws VaultException {
        int retryCount = 0;
        while (true) {
            try {
                final String requestJson = roleOptionsToJson(options);
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/pki/roles/" + roleName)
                        .header("X-Vault-Token", config.getToken())
                        .body(requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .post();

                // Validate restResponse
                if (restResponse.getStatus() != 204) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }
                return new LogicalResponse(restResponse, retryCount, new HashMap<String, String>());
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

    /**
     * TODO: Document
     *
     * TODO: Create return type
     *
     * @param roleName
     * @return
     * @throws VaultException
     */
    public LogicalResponse getRole(final String roleName) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/pki/roles/" + roleName)
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .get();

                // Validate response
                if (restResponse.getStatus() != 200 && restResponse.getStatus() != 404) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }

                final Map<String, String> data = restResponse.getBody() == null || restResponse.getBody().length == 0
                        ? new HashMap<String, String>()//NOPMD
                        : parseResponseData(restResponse);
                return new LogicalResponse(restResponse, retryCount, data);
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

    /**
     * TODO: Document
     *
     * TODO: Create return type
     *
     * @param roleName
     * @return
     */
    public LogicalResponse issue(
            final String roleName,
            final String commonName,
            final List<String> altNames,
            final List<String> ipSans,
            final Integer ttl,
            final String format    // TODO: Make enum
    ) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            final String requestJson = Json.object().add("common_name", commonName).toString();
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/pki/issue/" + roleName)
                        .header("X-Vault-Token", config.getToken())
                        .body(requestJson.getBytes("UTF-8"))
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .post();

                // Validate response
                if (restResponse.getStatus() != 200 && restResponse.getStatus() != 404) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }

                final Map<String, String> data = restResponse.getBody() == null || restResponse.getBody().length == 0
                        ? new HashMap<String, String>()//NOPMD
                        : parseResponseData(restResponse);
                return new LogicalResponse(restResponse, retryCount, data);
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

    /**
     * TODO: Document
     *
     * TODO: Create return type
     *
     * @param roleName
     * @return
     * @throws VaultException
     */
    public LogicalResponse deleteRole(final String roleName) throws VaultException {
        int retryCount = 0;
        while (true) {
            // Make an HTTP request to Vault
            try {
                final RestResponse restResponse = new Rest()//NOPMD
                        .url(config.getAddress() + "/v1/pki/roles/" + roleName)
                        .header("X-Vault-Token", config.getToken())
                        .connectTimeoutSeconds(config.getOpenTimeout())
                        .readTimeoutSeconds(config.getReadTimeout())
                        .sslPemUTF8(config.getSslPemUTF8())
                        .sslVerification(config.isSslVerify() != null ? config.isSslVerify() : null)
                        .delete();

                // Validate response
                if (restResponse.getStatus() != 204) {
                    throw new VaultException("Vault responded with HTTP status code: " + restResponse.getStatus());
                }

                final Map<String, String> data = restResponse.getBody() == null || restResponse.getBody().length == 0
                        ? new HashMap<String, String>()//NOPMD
                        : parseResponseData(restResponse);
                return new LogicalResponse(restResponse, retryCount, data);
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

    private String roleOptionsToJson(final RoleOptions options) {
        final JsonObject jsonObject = Json.object();
        if (options != null) {
            addJsonFieldIfNotNull(jsonObject, "ttl", options.getTtl());
            addJsonFieldIfNotNull(jsonObject, "max_ttl", options.getMaxTtl());
            addJsonFieldIfNotNull(jsonObject, "allow_localhost", options.getAllowLocalhost());
            if (options.getAllowedDomains() != null && options.getAllowedDomains().size() > 0) {
                final StringBuilder allowedDomains = new StringBuilder();
                for (int index = 0; index < options.getAllowedDomains().size(); index++) {
                    allowedDomains.append(options.getAllowedDomains().get(index));
                    if (index + 1 < options.getAllowedDomains().size()) {
                        allowedDomains.append(',');
                    }
                }
                addJsonFieldIfNotNull(jsonObject, "allowed_domains", allowedDomains.toString());
            }
            addJsonFieldIfNotNull(jsonObject, "allow_bare_domains", options.getAllowBareDomains());
            addJsonFieldIfNotNull(jsonObject, "allow_subdomains", options.getAllowSubdomains());
            addJsonFieldIfNotNull(jsonObject, "allow_any_name", options.getAllowAnyName());
            addJsonFieldIfNotNull(jsonObject, "enforce_hostnames", options.getEnforceHostnames());
            addJsonFieldIfNotNull(jsonObject, "allow_ip_sans", options.getAllowIpSans());
            addJsonFieldIfNotNull(jsonObject, "server_flag", options.getServerFlag());
            addJsonFieldIfNotNull(jsonObject, "client_flag", options.getClientFlag());
            addJsonFieldIfNotNull(jsonObject, "code_signing_flag", options.getCodeSigningFlag());
            addJsonFieldIfNotNull(jsonObject, "email_protection_flag", options.getEmailProtectionFlag());
            addJsonFieldIfNotNull(jsonObject, "key_type", options.getKeyType());
            addJsonFieldIfNotNull(jsonObject, "key_bits", options.getKeyBits());
            addJsonFieldIfNotNull(jsonObject, "use_csr_common_name", options.getUseCsrCommonName());
        }
        return jsonObject.toString();
    }

    private JsonObject addJsonFieldIfNotNull(final JsonObject jsonObject, final String name, final Object value) {
        if (value == null) {
            return jsonObject;
        }
        if (value instanceof String) {
            jsonObject.add(name, (String) value);
        } else if (value instanceof Boolean) {
            jsonObject.add(name, (Boolean) value);
        } else if (value instanceof Long) {
            jsonObject.add(name, (Long) value);
        }
        return jsonObject;
    }

    private Map<String, String> parseResponseData(final RestResponse restResponse) throws VaultException {
        final String mimeType = restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
        if (!mimeType.equals("application/json")) {
            throw new VaultException("Vault responded with MIME type: " + mimeType);
        }
        String jsonString;
        try {
            jsonString = new String(restResponse.getBody(), "UTF-8");//NOPMD
        } catch (UnsupportedEncodingException e) {
            throw new VaultException(e);
        }

        // Parse JSON
        final Map<String, String> data = new HashMap<String, String>();//NOPMD
        for (final JsonObject.Member member : Json.parse(jsonString).asObject().get("data").asObject()) {
            final JsonValue jsonValue = member.getValue();
            if (jsonValue == null || jsonValue.isNull()) {
                continue;
            } else if (jsonValue.isString()) {
                data.put(member.getName(), jsonValue.asString());
            } else {
                data.put(member.getName(), jsonValue.toString());
            }
        }
        return data;
    }
}
