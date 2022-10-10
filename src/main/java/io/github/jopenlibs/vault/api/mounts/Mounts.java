package io.github.jopenlibs.vault.api.mounts;

import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.OperationsBase;
import io.github.jopenlibs.vault.response.MountResponse;
import io.github.jopenlibs.vault.rest.Rest;
import io.github.jopenlibs.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;

/**
 * <p>The implementing class for operations on Vault's <code>/v1/sys/mounts/*</code> REST
 * endpoints.</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of
 * <code>Vault</code>
 * in a DSL-style builder pattern.  See the Javadoc comments of each <code>public</code> method for
 * usage examples.</p>
 */
public class Mounts extends OperationsBase {

    public Mounts(final VaultConfig config) {
        super(config);
    }

    /**
     * <p>Operation to list all the mounted secrets engines.  Relies on an authentication token
     * being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>The list of mount points information will be populated in the <code>mounts</code> field
     * of the <code>MountResponse</code> return value in the <code>Map&lt;String, Mount&gt;</code>
     * format.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final MountResponse response = vault.mounts().list();
     * final Map<String, Mount> mounts = response.getMounts();
     * }</pre>
     * </blockquote>
     *
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public MountResponse list() throws VaultException {
        return retry(attempt -> {
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(String.format("%s/v1/sys/mounts", config.getAddress()))
                    .header("X-Vault-Token", config.getToken())
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .get();

            // Validate restResponse
            if (restResponse.getStatus() != 200) {
                String body = restResponse.getBody() != null ? new String(restResponse.getBody())
                        : "(no body)";
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus() + " "
                                + body, restResponse.getStatus());
            }

            return new MountResponse(restResponse, attempt, true);
        });
    }

    /**
     * <p>Operation to enable secrets engine at given path.  Relies on an authentication token
     * being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>This method accepts a <code>MountConfig</code> parameter,   containing optional settings
     * for the mount creation operation. Example usage:</p>
     *
     * <p>A successful operation will return a 204 HTTP status.  A <code>VaultException</code> will
     * be thrown if  mount point already exists, or if any other problem occurs.  Example
     * usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final MountPayload payload = new MountPayload()
     *                                       .defaultLeaseTtl(TimeToLive.of(86400, TimeUnit.SECONDS))
     *                                       .maxLeaseTtl(TimeToLive.of(86400, TimeUnit.SECONDS))
     *                                       .description("description for pki engine");
     *
     * final MountResponse response = vault.mounts().enable("pki/mount/point/path", MountType.PKI, payload);
     *
     * assertEquals(204, response.getRestResponse().getStatus();
     * }</pre>
     * </blockquote>
     *
     * @param path The path to enable secret engine on.
     * @param type The type of secret engine to enable.
     * @param payload The <code>MountPayload</code> instance to use to create secret engine.
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public MountResponse enable(final String path, final MountType type, final MountPayload payload)
            throws VaultException {
        if (type == null) {
            throw new VaultException("Mount type is missing");
        }

        if (payload == null) {
            throw new VaultException("MountPayload is missing");
        }

        return retry(attempt -> {
            final String requestJson = payload.toEnableJson(type).toString();

            final RestResponse restResponse = new Rest()//NOPMD
                    .url(String.format("%s/v1/sys/mounts/%s", config.getAddress(), path))
                    .header("X-Vault-Token", config.getToken())
                    .body(requestJson.getBytes(StandardCharsets.UTF_8))
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .post();

            // Validate restResponse
            if (restResponse.getStatus() != 204) {
                String body =
                        restResponse.getBody() != null ? new String(restResponse.getBody())
                                : "(no body)";
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + " " + body, restResponse.getStatus());
            }

            return new MountResponse(restResponse, attempt, false);
        });
    }

    /**
     * <p>Operation to disable secrets engine mount point of given path.  Relies on an
     * authentication token being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>A successful operation will return a 204 HTTP status.  A <code>VaultException</code> will
     * be thrown if the mount point not exist, or if any other problem occurs.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final MountResponse response = vault.mounts().disable("pki/mount/point/path");
     *
     * assertEquals(204, response.getRestResponse().getStatus();
     * }</pre>
     * </blockquote>
     *
     * @param path The path to disable secret engine on.
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public MountResponse disable(final String path) throws VaultException {
        return retry(attempt -> {
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(String.format("%s/v1/sys/mounts/%s", config.getAddress(), path))
                    .header("X-Vault-Token", config.getToken())
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .delete();

            // Validate restResponse
            if (restResponse.getStatus() != 204) {
                String body =
                        restResponse.getBody() != null ? new String(restResponse.getBody())
                                : "(no body)";
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + " " + body, restResponse.getStatus());
            }

            return new MountResponse(restResponse, attempt, false);
        });
    }

    /**
     * <p>Operation to read secrets engine mount point's configuration of given path.  Relies on an
     * authentication token being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>The mount point information will be populated in the <code>mount</code> field of the
     * <code>MountResponse</code> return value.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final MountResponse response = vault.mounts().read("pki/mount/point/path");
     * final Mount mount = response.getMount();
     * final MountConfig mountConfig = mount.getConfig();
     * }</pre>
     * </blockquote>
     *
     * @param path The path to read secret engine's configuration from.
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public MountResponse read(final String path) throws VaultException {
        return retry(attempt -> {
            final RestResponse restResponse = new Rest()//NOPMD
                    .url(String.format("%s/v1/sys/mounts/%s/tune", config.getAddress(), path))
                    .header("X-Vault-Token", config.getToken())
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .get();

            // Validate restResponse
            if (restResponse.getStatus() != 200 && restResponse.getStatus() != 404) {
                String body =
                        restResponse.getBody() != null ? new String(restResponse.getBody())
                                : "(no body)";
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + " " + body, restResponse.getStatus());
            }

            return new MountResponse(restResponse, attempt, false);
        });
    }

    /**
     * <p>Operation to tune secrets engine mount point's configuration of given path.  Relies on an
     * authentication token being present in the <code>VaultConfig</code> instance.</p>
     *
     * <p>This the method accepts a <code>MountConfig</code> parameter, containing optional
     * settings for the  mount tune operation.  Example usage:</p>
     *
     * <p>A successful operation will return a 204 HTTP status.  A <code>VaultException</code> will
     * be thrown if the mount point not exist, or if any other problem occurs.  Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig.address(...).token(...).build();
     * final Vault vault = new Vault(config);
     *
     * final MountPayload payload = new MountPayload()
     *                                   .defaultLeaseTtl(TimeToLive.of(12, TimeUnit.HOURS))
     *                                   .maxLeaseTtl(TimeToLive.of(12, TimeUnit.HOURS))
     *                                   .description("description of pki");
     *
     * final MountResponse response = vault.mounts().tune("pki/mount/point/path", configs);
     *
     * assertEquals(204, response.getRestResponse().getStatus();
     * }</pre>
     * </blockquote>
     *
     * @param path The path to tune secret engine's configuration on.
     * @param payload The <code>MountPayload</code> instance to use to tune secret engine.
     * @return A container for the information returned by Vault
     * @throws VaultException If any error occurs or unexpected response is received from Vault
     */
    public MountResponse tune(final String path, final MountPayload payload) throws VaultException {
        return retry(attempt -> {
            if (payload == null) {
                throw new VaultException("MountPayload is missing");
            }

            final String requestJson = payload.toTuneJson().toString();

            final RestResponse restResponse = new Rest()//NOPMD
                    .url(String.format("%s/v1/sys/mounts/%s/tune", config.getAddress(), path))
                    .header("X-Vault-Token", config.getToken())
                    .body(requestJson.getBytes(StandardCharsets.UTF_8))
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .post();

            // Validate restResponse
            if (restResponse.getStatus() != 204) {
                String body =
                        restResponse.getBody() != null ? new String(restResponse.getBody())
                                : "(no body)";
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + " " + body, restResponse.getStatus());
            }

            return new MountResponse(restResponse, attempt, false);
        });
    }
}
