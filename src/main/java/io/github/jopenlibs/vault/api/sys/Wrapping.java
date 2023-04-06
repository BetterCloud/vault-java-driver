package io.github.jopenlibs.vault.api.sys;

import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
import io.github.jopenlibs.vault.api.OperationsBase;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.response.UnwrapResponse;
import io.github.jopenlibs.vault.response.WrapResponse;
import io.github.jopenlibs.vault.rest.Rest;
import io.github.jopenlibs.vault.rest.RestResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <p>The implementing class for <code>/v1/sys/wrapping/*</code> REST endpoints</p>
 *
 * <p>This class is not intended to be constructed directly.  Rather, it is meant to used by way of
 * <code>Vault</code> in a DSL-style builder pattern. See the Javadoc comments of each
 * <code>public</code> method for usage examples.</p>
 *
 * @see Sys#wrapping()
 */
public class Wrapping extends OperationsBase {

    private String nameSpace;

    public Wrapping(final VaultConfig config) {
        super(config);

        if (this.config.getNameSpace() != null && !this.config.getNameSpace().isEmpty()) {
            this.nameSpace = this.config.getNameSpace();
        }
    }

    /**
     * <p>Returns information about the current client token for a wrapped token, for which the
     * lookup endpoint is at "sys/wrapping/lookup". Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final String wrappingToken = "...";
     * final VaultConfig config = new VaultConfig().address(...).token(wrappingToken).build();
     * final Vault vault = new Vault(config);
     * final LogicalResponse response = vault.sys().wrapping().lookupWarp();
     * // Then you can validate "path" for example ...
     * final String path = response.getData().get("path");
     * }</pre>
     * </blockquote>
     *
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse lookupWrap() throws VaultException {
        return lookupWrap(config.getToken(), false);
    }

    /**
     * <p>Returns information about the a wrapped token when authorization is needed for lookup,
     * for which the lookup endpoint is at "sys/wrapping/lookup". Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig().address(...).token(authToken).build();
     * final Vault vault = new Vault(config);
     * ...
     * final String wrappingToken = "...";
     * final LogicalResponse response = vault.sys().wrapping().lookupWarp(wrappingToken);
     * // Then you can validate "path" for example ...
     * final String path = response.getData().get("path");
     * }</pre>
     * </blockquote>
     *
     * @param wrappedToken Wrapped token.
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse lookupWrap(final String wrappedToken) throws VaultException {
        return lookupWrap(wrappedToken, true);
    }

    /**
     * <p>Returns information about the a wrapped token,
     * for which the lookup endpoint is at "sys/wrapping/lookup". Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final VaultConfig config = new VaultConfig().address(...).token(authToken).build();
     * final Vault vault = new Vault(config);
     * ...
     * final String wrappingToken = "...";
     * final LogicalResponse response = vault.sys().wrapping().lookupWarp(wrappingToken);
     * // Then you can validate "path" for example ...
     * final String path = response.getData().get("path");
     * }</pre>
     * </blockquote>
     *
     * @param wrappedToken Wrapped token.
     * @param inBody When {@code true} the token value placed in the body request:
     * {@code {"token": "$wrappedToken"}}, otherwise, set the token into header:
     * {@code "X-Vault-Token: $wrappedToken"}.
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     */
    public LogicalResponse lookupWrap(final String wrappedToken, boolean inBody)
            throws VaultException {
        final String requestJson =
                inBody ? Json.object().add("token", wrappedToken).toString() : null;

        return retry(attempt -> {
            // HTTP request to Vault
            Rest rest = new Rest()//NOPMD
                    .url(config.getAddress() + "/v1/sys/wrapping/lookup")
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext());

            if (inBody) {
                rest = rest
                        .header("X-Vault-Token", config.getToken())
                        .body(requestJson.getBytes(StandardCharsets.UTF_8));
            } else {
                rest = rest.header("X-Vault-Token", wrappedToken);
            }

            final RestResponse restResponse = rest.post();

            // Validate restResponse
            if (restResponse.getStatus() != 200) {
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus() +
                                "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8),
                        restResponse.getStatus());
            }

            final String mimeType = restResponse.getMimeType();
            if (!"application/json".equals(mimeType)) {
                throw new VaultException("Vault responded with MIME type: " + mimeType,
                        restResponse.getStatus());
            }

            return new LogicalResponse(restResponse, attempt,
                    Logical.logicalOperations.authentication);
        });
    }

    /**
     * <p>Provide access to the {@code /sys/wrapping/wrap} endpoint.</p>
     *
     * <p>This provides a powerful mechanism for information sharing in many environments.
     * In the types of scenarios, often the best practical option is to provide cover for the secret
     * information, be able to detect malfeasance (interception, tampering), and limit lifetime of
     * the secret's exposure. Response wrapping performs all three of these duties:</p>
     *
     * <ul>
     *     <li>It provides cover by ensuring that the value being transmitted across the wire is
     *     not the actual secret but a reference to such a secret, namely the response-wrapping token.
     *     Information stored in logs or captured along the way do not directly see the sensitive information.
     *     </li>
     *     <li>It provides malfeasance detection by ensuring that only a single party can ever
     *     unwrap the token and see what's inside. A client receiving a token that cannot be unwrapped
     *     can trigger an immediate security incident. In addition, a client can inspect
     *     a given token before unwrapping to ensure that its origin is from the expected
     *     location in Vault.
     *     </li>
     *     <li>It limits the lifetime of secret exposure because the response-wrapping token has
     *     a lifetime that is separate from the wrapped secret (and often can be much shorter),
     *     so if a client fails to come up and unwrap the token, the token can expire very quickly.
     *     </li>
     * </ul>
     *
     * <blockquote>
     * <pre>{@code
     * final String authToken = "...";
     * final String wrappingToken = "...";
     * final VaultConfig config = new VaultConfig().address(...).token(authToken).build();
     * final Vault vault = new Vault(config);
     *
     * final WrapResponse wrapResponse = vault.sys().wrapping().wrap(
     *                 // Data to wrap
     *                 new JsonObject()
     *                         .add("foo", "bar")
     *                         .add("zoo", "zar"),
     *
     *                 // TTL of the response-wrapping token
     *                 60
     *         );
     *
     * final UnwrapResponse unwrapResponse = vault.sys().wrapping().unwrap(wrapResponse.getToken());
     * final JsonObject unwrappedData = response.getData(); // original data
     * }</pre>
     * </blockquote>
     *
     * @param jsonObject User data to wrap.
     * @param ttlInSec Wrap TTL in seconds
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     * @see #unwrap(String)
     */
    public WrapResponse wrap(final JsonObject jsonObject, int ttlInSec) throws VaultException {
        Objects.requireNonNull(jsonObject);

        return retry(attempt -> {
            // Parse parameters to JSON
            final String requestJson = jsonObject.toString();
            final String url = config.getAddress() + "/v1/sys/wrapping/wrap";

            // HTTP request to Vault
            final RestResponse restResponse = new Rest()
                    .url(url)
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Wrap-TTL", Integer.toString(ttlInSec))
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .body(requestJson.getBytes(StandardCharsets.UTF_8))
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .post();

            // Validate restResponse
            if (restResponse.getStatus() != 200) {
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8),
                        restResponse.getStatus());
            }

            final String mimeType =
                    restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
            if (!mimeType.equals("application/json")) {
                throw new VaultException("Vault responded with MIME type: " + mimeType,
                        restResponse.getStatus());
            }

            return new WrapResponse(restResponse, attempt);
        });
    }

    /**
     * <p>Returns the original response inside the wrapped auth token. This method is useful if you
     * need to unwrap a token without being authenticated. See {@link #unwrap(String)} if you need
     * to do that authenticated.</p>
     *
     * <p>In the example below, you cannot use twice the {@code VaultConfig}, since
     * after the first usage of the {@code wrappingToken}, it is not usable anymore. You need to use
     * the {@code unwrappedToken} in a new vault configuration to continue. Example usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final String wrappingToken = "...";
     * final VaultConfig config = new VaultConfig().address(...).token(wrappingToken).build();
     * final Vault vault = new Vault(config);
     * final AuthResponse response = vault.sys().wrapping().unwrap();
     * final String unwrappedToken = response.getAuthClientToken();
     * }</pre>
     * </blockquote>
     *
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     * @see #unwrap(String)
     */
    public UnwrapResponse unwrap() throws VaultException {
        return unwrap(config.getToken(), false);
    }

    /**
     * <p>Provide access to the {@code /sys/wrapping/unwrap} endpoint.</p>
     *
     * <p>Returns the original response inside the given wrapping token. Unlike simply reading
     * {@code cubbyhole/response} (which is deprecated), this endpoint provides additional
     * validation checks on the token, returns the original value on the wire rather than a JSON
     * string representation of it, and ensures that the response is properly audit-logged.</p>
     *
     * <p> This endpoint can be used by using a wrapping token as the client token in the API call,
     * in which case the token parameter is not required; or, a different token with permissions to
     * access this endpoint can make the call and pass in the wrapping token in the token parameter.
     * Do not use the wrapping token in both locations; this will cause the wrapping token to be
     * revoked but the value to be unable to be looked up, as it will basically be a double-use of
     * the token!</p>
     *
     * <p>In the example below, {@code authToken} is NOT your wrapped token, and should have
     * unwrapping permissions. The unwrapped data in {@link UnwrapResponse#getData()}. Example
     * usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final String authToken = "...";
     * final String wrappingToken = "...";
     * final VaultConfig config = new VaultConfig().address(...).token(authToken).build();
     * final Vault vault = new Vault(config);
     *
     * final WrapResponse wrapResponse = vault.sys().wrapping().wrap(
     *                 // Data to wrap
     *                 new JsonObject()
     *                         .add("foo", "bar")
     *                         .add("zoo", "zar"),
     *
     *                 // TTL of the response-wrapping token
     *                 60
     *         );
     *
     * final UnwrapResponse unwrapResponse = vault.sys().wrapping().unwrap(wrapResponse.getToken());
     * final JsonObject unwrappedData = response.getData(); // original data
     * }</pre>
     * </blockquote>
     *
     * @param wrappedToken Specifies the wrapping token ID, do NOT also put this in your
     * {@link VaultConfig#getToken()}, if token is {@code null}, this method will unwrap the auth
     * token in {@link VaultConfig#getToken()}
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     * @see #wrap(JsonObject, int)
     * @see #unwrap()
     */
    public UnwrapResponse unwrap(final String wrappedToken) throws VaultException {
        return unwrap(wrappedToken, true);
    }

    /**
     * <p>Provide access to the {@code /sys/wrapping/unwrap} endpoint.</p>
     *
     * <p>Returns the original response inside the given wrapping token. Unlike simply reading
     * {@code cubbyhole/response} (which is deprecated), this endpoint provides additional
     * validation checks on the token, returns the original value on the wire rather than a JSON
     * string representation of it, and ensures that the response is properly audit-logged.</p>
     *
     * <p> This endpoint can be used by using a wrapping token as the client token in the API call,
     * in which case the token parameter is not required; or, a different token with permissions to
     * access this endpoint can make the call and pass in the wrapping token in the token parameter.
     * Do not use the wrapping token in both locations; this will cause the wrapping token to be
     * revoked but the value to be unable to be looked up, as it will basically be a double-use of
     * the token!</p>
     *
     * <p>In the example below, {@code authToken} is NOT your wrapped token, and should have
     * unwrapping permissions. The unwrapped data in {@link UnwrapResponse#getData()}. Example
     * usage:</p>
     *
     * <blockquote>
     * <pre>{@code
     * final String authToken = "...";
     * final String wrappingToken = "...";
     * final VaultConfig config = new VaultConfig().address(...).token(authToken).build();
     * final Vault vault = new Vault(config);
     *
     * final WrapResponse wrapResponse = vault.sys().wrapping().wrap(
     *                 // Data to wrap
     *                 new JsonObject()
     *                         .add("foo", "bar")
     *                         .add("zoo", "zar"),
     *
     *                 // TTL of the response-wrapping token
     *                 60
     *         );
     *
     * final UnwrapResponse unwrapResponse = vault.sys().wrapping().unwrap(wrapResponse.getToken(), true);
     * final JsonObject unwrappedData = response.getData(); // original data
     * }</pre>
     * </blockquote>
     *
     * @param wrappedToken Specifies the wrapping token ID, do NOT also put this in your
     * {@link VaultConfig#getToken()}, if token is {@code null}, this method will unwrap the auth
     * token in {@link VaultConfig#getToken()}
     * @param inBody When {@code true} the token value placed in the body request:
     * {@code {"token": "$wrappedToken"}}, otherwise, set the token into header:
     * {@code "X-Vault-Token: $wrappedToken"}.
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     * @see #wrap(JsonObject, int)
     * @see #unwrap()
     */
    public UnwrapResponse unwrap(final String wrappedToken, boolean inBody) throws VaultException {
        Objects.requireNonNull(wrappedToken, "Wrapped token is null");

        return retry(attempt -> {
            final String url = config.getAddress() + "/v1/sys/wrapping/unwrap";

            // HTTP request to Vault
            Rest rest = new Rest()
                    .url(url)
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext());

            if (inBody) {
                final String requestJson = Json.object().add("token", wrappedToken).toString();
                rest = rest
                        .header("X-Vault-Token", config.getToken())
                        .body(requestJson.getBytes(StandardCharsets.UTF_8));
            } else {
                rest = rest
                        .header("X-Vault-Token", wrappedToken);
            }

            RestResponse restResponse = rest.post();

            // Validate restResponse
            if (restResponse.getStatus() != 200) {
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8),
                        restResponse.getStatus());
            }

            final String mimeType =
                    restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();

            if (!mimeType.equals("application/json")) {
                throw new VaultException("Vault responded with MIME type: " + mimeType,
                        restResponse.getStatus());
            }

            return new UnwrapResponse(restResponse, attempt);
        });
    }

    /**
     * <p>Provide access to the {@code /sys/wrapping/rewrap} endpoint. This endpoint rewraps a
     * response-wrapped token. The new token will use the same creation TTL as the original token
     * and contain the same response. The old token will be invalidated. This can be used for
     * long-term storage of a secret in a response-wrapped token when rotation is a
     * requirement.</p>
     *
     * <blockquote>
     * <pre>{@code
     * final String authToken = "...";
     * final String wrappingToken = "...";
     * final VaultConfig config = new VaultConfig().address(...).token(authToken).build();
     * final Vault vault = new Vault(config);
     *
     * final WrapResponse wrapResponse = vault.auth().wrap(
     *                 // Data to wrap
     *                 new JsonObject()
     *                         .add("foo", "bar")
     *                         .add("zoo", "zar"),
     *
     *                 // TTL of the response-wrapping token
     *                 60
     *         );
     * ...
     * final WrapResponse wrapResponse2 = vault.auth().rewrap(wrapResponse.getToken());
     *
     * final UnwrapResponse unwrapResponse = vault.auth().unwrap(wrapResponse2.getToken());
     * final JsonObject unwrappedData = response.getData(); // original data
     * }</pre>
     * </blockquote>
     *
     * @param wrappedToken Wrapped token ID to re-wrap.
     * @return The response information returned from Vault
     * @throws VaultException If any error occurs, or unexpected response received from Vault
     * @see #wrap(JsonObject, int)
     */
    public WrapResponse rewrap(final String wrappedToken) throws VaultException {
        Objects.requireNonNull(wrappedToken);

        return retry(attempt -> {
            // Parse parameters to JSON
            final String requestJson = Json.object().add("token", wrappedToken).toString();
            final String url = config.getAddress() + "/v1/sys/wrapping/rewrap";

            // HTTP request to Vault
            final RestResponse restResponse = new Rest()
                    .url(url)
                    .header("X-Vault-Token", config.getToken())
                    .header("X-Vault-Namespace", this.nameSpace)
                    .header("X-Vault-Request", "true")
                    .body(requestJson.getBytes(StandardCharsets.UTF_8))
                    .connectTimeoutSeconds(config.getOpenTimeout())
                    .readTimeoutSeconds(config.getReadTimeout())
                    .sslVerification(config.getSslConfig().isVerify())
                    .sslContext(config.getSslConfig().getSslContext())
                    .post();

            // Validate restResponse
            if (restResponse.getStatus() != 200) {
                throw new VaultException(
                        "Vault responded with HTTP status code: " + restResponse.getStatus()
                                + "\nResponse body: " + new String(restResponse.getBody(),
                                StandardCharsets.UTF_8),
                        restResponse.getStatus());
            }

            final String mimeType =
                    restResponse.getMimeType() == null ? "null" : restResponse.getMimeType();
            if (!mimeType.equals("application/json")) {
                throw new VaultException("Vault responded with MIME type: " + mimeType,
                        restResponse.getStatus());
            }

            return new WrapResponse(restResponse, attempt);
        });
    }
}
