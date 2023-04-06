package io.github.jopenlibs.vault.api.sys;

import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.OperationsBase;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.response.SealResponse;
import io.github.jopenlibs.vault.rest.Rest;
import io.github.jopenlibs.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;

/**
 * <p>The implementing class for operations on REST endpoints, under the "seal/unseal/seal-status"
 * section of the Vault HTTP API docs (<a
 * href="https://www.vaultproject.io/api/system/index.html">https://www.vaultproject.io/api/system/index.html</a>).
 * </p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of
 * <code>Vault</code> in a DSL-style builder pattern.  See the Javadoc comments of each
 * <code>public</code> method for usage examples.</p>
 */
public class Seal extends OperationsBase {

    private String nameSpace;

    public Seal(final VaultConfig config) {
        super(config);

        if (this.config.getNameSpace() != null && !this.config.getNameSpace().isEmpty()) {
            this.nameSpace = this.config.getNameSpace();
        }
    }

    public Seal withNameSpace(final String nameSpace) {
        this.nameSpace = nameSpace;
        return this;
    }

    /**
     * <p>Seal the Vault.</p>
     *
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public SealResponse seal() throws VaultException {
        return retry((attempt) -> {
            // HTTP request to Vault
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/sys/seal")
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .post();

            return getSealResponse(attempt, restResponse, 204);
        });
    }

    /**
     * <p>Enter a single master key share to progress the unsealing of the Vault.</p>
     *
     * @param key Single master key share
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public SealResponse unseal(final String key) throws VaultException {
        return unseal(key, false);
    }


    /**
     * <p>Enter a single master key share to progress the unsealing of the Vault.</p>
     *
     * @param key Single master key share
     * @param reset Specifies if previously-provided unseal keys are discarded and the unseal
     * process is reset
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public SealResponse unseal(final String key, final Boolean reset) throws VaultException {
        return retry((attempt) -> {
            // HTTP request to Vault
            final String requestJson = Json.object().add("key", key).add("reset", reset)
                    .toString();
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/sys/unseal")
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .body(requestJson.getBytes(StandardCharsets.UTF_8))
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .post();

            // Validate restResponse
            return getSealResponse(attempt, restResponse, 200);
        });
    }

    /**
     * <p>Check progress of unsealing the Vault.</p>
     *
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public SealResponse sealStatus() throws VaultException {
        return retry((attempt) -> {
            // HTTP request to Vault
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/sys/seal-status")
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .get();

            // Validate restResponse
            return getSealResponse(attempt, restResponse, 200);
        });
    }

    private SealResponse getSealResponse(final int retryCount, final RestResponse restResponse,
            final int expectedResponse) throws VaultException {
        if (restResponse.getStatus() != expectedResponse) {
            throw new VaultException(
                    "Vault responded with HTTP status code: " + restResponse.getStatus(),
                    restResponse.getStatus());
        }

        final String mimeType = String.valueOf(restResponse.getMimeType());
        if (!mimeType.equals("application/json")) {
            throw new VaultException("Vault responded with MIME type: " + mimeType,
                    restResponse.getStatus());
        }
        return new SealResponse(restResponse, retryCount);
    }
}
