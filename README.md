Vault Java Driver
=================
A zero-dependency Java client for the [Vault](https://www.vaultproject.io/) secrets management solution from
HashiCorp.

This driver strives to implement Vault's full HTTP API, along with supporting functionality such as automatic
retry handling.  It does so without relying on any other external libraries beyond the Java standard library,
and is compatible with Java 8 and up.  So it will play nice with all of your projects, greenfield and legacy
alike, without causing conflicts with any other dependency.

NOTE:  Although the binary artifact produced by the project is backwards-compatible with Java 8, you do need
       JDK 9 or higher to modify or build the source code of this library itself.

This Change
-----------

Table of Contents
-----------------
* [Installing the Driver](#installing-the-driver)
* [Initializing a Driver Instance](#initializing-a-driver-instance)
* [Key/Value Secret Engine Config](#key-value-secret-engine-config)
* [SSL Config](#ssl-config)
  * [General Options](#general-options)
  * [Java Keystore (JKS) based config](#java-keystore-jks-based-config)
  * [OpenSSL (PEM) based config](#openssl-pem-based-config)
* [Using the driver](#using-the-driver)
* [API Reference (Javadocs)](#api-reference-javadocs)
* [Version History](#version-history)
* [Development](#development)
* [License](#license)
* [Other Notes](#other-notes)

Installing the Driver
---------------------
The driver is available from Maven Central, for all modern Java build systems.

Gradle:
```
dependencies {
    implementation 'io.ianferguson:vault-java-driver:6.0.0'
}
```

Maven:
```
<dependency>
    <groupId>io.ianferguson</groupId>
    <artifactId>vault-java-driver</artifactId>
    <version>6.0.0</version>
</dependency>
```

Initializing a Driver Instance
------------------------------
The `io.ianferguson.vault.VaultConfig` class is used to initialize a driver instance with desired settings.
In the most basic use cases, where you are only supplying a Vault server address and perhaps a root token, there
are convenience constructor methods available:
```
final VaultConfig config = new VaultConfig()
                                  .address("http://127.0.0.1:8200")
                                  .token("3c9fd6be-7bc2-9d1f-6fb3-cd746c0fc4e8")
                                  .build();

// You may choose not to provide a root token initially, if you plan to use
// the Vault driver to retrieve one programmatically from an auth backend.
final VaultConfig config = new VaultConfig().address("http://127.0.0.1:8200").build();
```

To explicitly set additional config parameters (*), you can use a builder pattern style to construct the `VaultConfig`
instance.  Either way, the initialization process will try to populate any unset values by looking to
environment variables.

```
final VaultConfig config =
    new VaultConfig()
        .address("http://127.0.0.1:8200")               // Defaults to "VAULT_ADDR" environment variable
        .token("3c9fd6be-7bc2-9d1f-6fb3-cd746c0fc4e8")  // Defaults to "VAULT_TOKEN" environment variable
        .openTimeout(5)                                 // Defaults to "VAULT_OPEN_TIMEOUT" environment variable
        .readTimeout(30)                                // Defaults to "VAULT_READ_TIMEOUT" environment variable
        .sslConfig(new SslConfig().build())             // See "SSL Config" section below
        .build();
```

Once you have initialized a `VaultConfig` object, you can use it to construct an instance of the `Vault` primary
driver class:

```
final Vault vault = new Vault(config);
```

Key Value Secret Engine Config
------------------------------
Shortly before its `1.0` release, Vault added a Version 2 of its [Key/Value Secrets Engine](https://www.vaultproject.io/docs/secrets/kv/index.html).  This
supports some addition features beyond the Version 1 that was the default in earlier Vault builds (e.g. secret rotation, soft deletes, etc).

Unfortunately, K/V V2 introduces some breaking changes, in terms of both request/response payloads as well as how URL's are constructed
for Vault's REST API.  Therefore, version `4.0.0` of this Vault Driver likewise had to introduce some breaking changes, to allow support
for both K/V versions.

* **If you are using the new K/V V2 across the board**, then no action is needed.  The Vault Driver now assumes this by default.

* **If you are still using the old K/V V1 across the board**, then you can use the `Vault` class constructor:
  `public Vault(final VaultConfig vaultConfig, final Integer engineVersion)`, supplying a `1` as the engine version parameter.
  constructor, then you can declare whether to use Version 1 or 2 across the board.

* **If you are using a mix, of some secret paths mounted with V1 and others mounted with V2**, then you have two options:

  * You can explicitly specify your Vault secret paths, and which K/V version each one is using.  Construct your `Vault` objects
    with the constructor `public Vault(final VaultConfig vaultConfig, final Boolean useSecretsEnginePathMap, final Integer globalFallbackVersion)`.
    Within the `VaultConfig` object, supply a map of Vault secret paths to their associated K/V version (`1` or `2`).

  * You can rely on the Vault Driver to auto-detect your mounts and K/V versions upon instantiation.  Use the same constructor as above,
    but leave the map `null`.  Note that this option requires your authentication credentials to have access to read Vault's `/v1/sys/mounts`
    path.

Version 2 of the K/V engine dynamically injects a qualifier element into your secret paths, which varies depending on the type of for read and write operations, in between the root version
operation.  For example, for read and write operations, the secret path:

```v1/mysecret```

... has a "data" qualifier injected:

```v1/data/mysecret```

The default behavior of this driver is to insert the appropriate qualifier one level deep (i.e. in between the root version number
and the rest of the path).  However, if your secret path is prefixed, such that the qualifier should be injected further down:

```v1/my/long/prefix/data/anything/else```

... then you should accordingly set the `VaultConfig.prefixPathDepth` property when constructing your `Vault` instance.


SSL Config
----------
If your Vault server uses a SSL certificate, then you must supply that certificate to establish connections.  Also, if
you are using certificate-based client authentication, then you must supply a client certificate and private key that
have been previously registered with your Vault server.

SSL configuration has been broken off from the `VaultConfig` class, and placed in its own `SslConfig` class.  This
class likewise using a builder pattern.

#### General Options

```
.verify(false)    // Defaults to "VAULT_SSL_VERIFY" environment variable (or else "true")
```

To disable SSL certificate verification altogether, set `sslVerify(false)`.  YOU SHOULD NOT DO THIS IS A REAL
PRODUCTION SETTING!  However, it can be useful in a development or testing server context.  If this value is
explicitly set to `false`, then all other SSL config is basically unused.

#### Java Keystore (JKS) based config

You can provide the driver with a JKS truststore, containing Vault's server-side certificate for basic SSL,
using one of the following three options:

`.trustStore(object)`       - Supply an in-memory `java.security.KeyStore` file, containing Vault server cert(s) that
                              can be trusted.

`.trustStoreFile(path)`     - Same as the above, but the path references a JKS file on the filesystem.

`.trustStoreResource(path)` - Same as the above, but the path references a classpath resource rather than a filesystem
                              path (e.g. if you've bundled the JKS file into your application's JAR, WAR, or EAR file).

If you are only using basic SSL, then no keystore need be provided.  However, if you would like to use Vault's
TLS Certificate auth backend for client side auth, then you need to provide a JKS keystore containing your
client-side certificate and private key:

`.keyStore(object, password)`       - Supply an in-memory `java.security.KeyStore` file containing a client
                                      certificate and private key, and the password needed to access it (can be null).
                              can be trusted.

`.keyStoreFile(path, password)`     - Same as the above, but the path references a JKS file on the filesystem.

`.keyStoreResource(path, password)` - Same as the above, but the path references a classpath resource rather than a
                                      filesystem path (e.g. if you've bundled the JKS file into your application's JAR,
                                      WAR, or EAR file).

NOTE:  JKS-based config trumps PEM-based config (see below).  If for some reason you build an `SslConfig` object
with both JKS and PEM data present, then only the JKS data will be used.  You cannot "mix-and-match", providing
a JKS-based truststore and PEM-based client auth data.

#### OpenSSL (PEM) based config

To supply Vault's server-side certificate for basic SSL, you can use one of the following three options:

`.pemFile(path)` - Supply the path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding (defaults
                   to "VAULT_SSL_CERT" environment variable).

`.pemResource(path)` - Same as above, but the path references a classpath resource rather than a filesystem path (e.g. if
                       you've bundled the PEM file into your applications's JAR, WAR, or EAR file).

`.pemUTF8(contents)` - The string contents extracted from the PEM file.  For Java to parse the certificate properly,
                       there must be a line-break in between the certificate header and body (see the `SslConfig`
                       Javadocs for more detail).

If SSL verification is enabled, no JKS-based config is provided, AND none of these three methods are called,
then `SslConfig` will by default check for a `VAULT_SSL_CERT` environment variable.  If that's setw then it will be
treated as a filesystem path.

To use Vault's TLS Certificate auth backend for SSL client auth, you must provide your client certificate and
private key, using some pair from the following options:

`.clientPemUTF8(path)` - Supply the path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding.

`.clientPemResource(path)` - Same as above, but the path references a classpath resource rather than a filesystem path (e.g. if
                       you've bundled the PEM file into your applications's JAR, WAR, or EAR file).

`.clientPemUTF8(contents)` - The string contents extracted from the PEM file.  For Java to parse the certificate properly,
                       there must be a line-break in between the certificate header and body (see the `SslConfig`
                       Javadocs for more detail).

`.clientKeyPemUTF8(path)` - Supply the path to an RSA private key in unencrypted PEM format, using UTF-8 encoding.

`.clientKeyPemResource(path)` - Same as above, but the path references a classpath resource rather than a filesystem path (e.g. if
                       you've bundled the PEM file into your applications's JAR, WAR, or EAR file).

`.clientKeyPemUTF8(contents)` - The string contents extracted from the PEM file.  For Java to parse the certificate properly,
                       there must be a line-break in between the certificate header and body (see the `SslConfig`
                       Javadocs for more detail).



Using the Driver
----------------
Like the `VaultConfig` class, `Vault` too supports a builder pattern DSL style:

```
final Map<String, String> secrets = new HashMap<String, String>();
secrets.put("value", "world");
secrets.put("other_value", "You can store multiple name/value pairs under a single key");

// Write operation
final LogicalResponse writeResponse = vault.logical()
                                        .write("secret/hello", secrets);

...

// Read operation
final String value = vault.logical()
                       .read("secret/hello")
                       .getData().get("value");
```

`Vault` has a number of methods for accessing the classes that implement the various endpoints of Vault's HTTP API:

* `logical()`:  Contains core operations such as reading and writing secrets.
* `auth()`:  Exposes methods for working with Vault's various auth backends (e.g. to programmatically retrieve a token
  by authenticating with a username and password).
* `pki()`: Operations on the PKI backend (e.g. create and delete roles, issue certificate credentials).
* `debug()`: Health check endpoints.

The driver DSL also allows you to specify retry logic, by chaining the `withRetries()` ahead of accessing the endpoint
implementation:

```
// Retry up to 5 times if failures occur, waiting 1000 milliseconds in between each retry attempt.
final LogicalResponse response = vault.withRetries(5, 1000)
                                   .logical()
                                   .read("secret/hello");
```

API Reference (Javadocs)
------------------------
Full [Javadoc documentation](http://ianferguson.github.io/vault-java-driver/javadoc/).

Version History
---------------

Note that changes to the major version (i.e. the first number) represent possible breaking changes, and
may require modifications in your code to migrate.  Changes to the minor version (i.e. the second number)
should represent non-breaking changes.  The third number represents any very minor bugfix patches.

* **5.1.0**:  This release contains the following updates:
  * Supports path prefixes when using K/V engine V2.  [(PR #189)](https://github.com/BetterCloud/vault-java-driver/pull/189)
  * Fixes issues with bulk requests in the transit API.  [(PR #195)](https://github.com/BetterCloud/vault-java-driver/pull/195)
  * Adds response body to exception for Auth failures.  [(PR #198)](https://github.com/BetterCloud/vault-java-driver/pull/198)
  * Support all options for the createToken operation.  [(PR #199)](https://github.com/BetterCloud/vault-java-driver/pull/199)

* **5.0.0**:  This release contains the following updates:
  * Changes the retry behavior, to no longer attempt retries on 4xx response codes (for which retries generally won't succeed anyway).  This
    is the only (mildly) breaking change in this release, necessitating a major version bump. [(PR #176)](https://github.com/BetterCloud/vault-java-driver/pull/176)
  * Implements support for the Database secret engine. [(PR #175)](https://github.com/BetterCloud/vault-java-driver/pull/175)
  * Makes the "x-vault-token" header optional, to allow use of Vault Agent.  [(PR #184)](https://github.com/BetterCloud/vault-java-driver/pull/184)
  * Removes stray uses of `System.out.println` in favor of `java.util.logging`. [(PR #178)](https://github.com/BetterCloud/vault-java-driver/pull/178)
  * Adds the enum constant `MountType.KEY_VALUE_V2`.  [(PR #182)](https://github.com/BetterCloud/vault-java-driver/pull/182)

* **4.1.0**:  This release contains the following updates:
  * Support for JWT authentication, for use by Kubernetes and other JWT-based authentication providers.  [(PR #164)](https://github.com/BetterCloud/vault-java-driver/pull/164)
  * Updates the lease revoke method, to support changes in the underlying Vault API.  [(PR #163)](https://github.com/BetterCloud/vault-java-driver/pull/163)
  * Changes the `VaultConfig.secretsEnginePathMap(...)` method from default access level to `public`, to allow for manual
    setting [(PR #164)](https://github.com/BetterCloud/vault-java-driver/pull/156)
  * Adds the nonce value to `AuthResponse`, to facilitate re-authentication with Vault via AWS.  [(PR #168)](https://github.com/BetterCloud/vault-java-driver/pull/168)
  * Establishes a `module-info` file, updates the JDK requirement for building this library to Java 9 (although the built
    library artifact remains compatible as a dependency in Java 8 projects).  [(PR #165)](https://github.com/BetterCloud/vault-java-driver/pull/165)
  * Updates Gradle, and various test dependencies to their latest versions.  Integration tests now target Vault 1.1.3.

* **4.0.0**:  This is a breaking-change release, with two primary updates:
  * Adds support for Version 2 of the Key/Value Secrets Engine.  The driver now assumes that your Vault instance uses Version 2 of the
    Key/Value Secrets Engine across the board.  To configure this, see the [Key/Value Secret Engine Config](#key-value-secret-engine-config)
    section above.
  * Adds support for the namespaces feature of Vault Enterprise.

* **3.1.0**:  Several updates.
  * Adds support for seal-related operations (i.e. `/sys/seal`, `/sys/unseal`, `/sys/seal-status`).
  * Adds support for the AWS auth backend.
  * Adds support for the Google Cloud Platform auth backend.
  * Adds support for the LDAP auth Backend.
  * Allows auth backend methods to be configured for non-default mount points.
  * Adds "revoke-self" capability for auth tokens.
  * Adds support for response-wrapping token validation
  * Support for signing a new certificate based on a CSR (i.e. the `/v1/pki/sign` endpoint).
  * Support for the PKI backend revoke method, and addition of a useCsrSans property in PKI role object
  * Gives `VaultConfig` the ability to disable loading from environment variables if desired.
  * Cleans up issues with the new Docker-based integration test suite.
  * Updates all dependencies to their latest versions (including switching to Vault 0.9.1 for integration testing).

* **3.0.0**: This is a breaking-change release, with several updates.
  * **API changes**:
    * Adds support for writing arbitrary objects to Vault, instead of just strings (i.e. the
      `com.bettercloud.vault.api.Logical.write(...)` method now accepts a `Map<String. Object>` rather than a
      `Map<String, String>`).
    * Refactors the `VaultConfig` class, forcing use of the builder pattern and breaking off SSL-related
      config into a separate `SslConfig` class.
    * Refactors the `Auth.createToken()` method, to encapsulate the possible options within a config object
      rather than having the method signature contain 8 optional arguments.
  * **SSL and Auth Backend support**:
    * Adds support for authenticating with the TLS Certificate auth backend.
    * Updates SSL support in general, allowing users to configure the driver with Java-friendly JKS keystore
      and truststore files (in addition to continuing to support Vault-friendly PEM format).
    * Implements the `/v1/auth/token/lookup-self` endpoint.
    * Supports creating tokens against a role.
  * **Major re-vamp of the integration test suite**:
    * The tests now use the [TestContainers](https://www.testcontainers.org/) library to setup and launch a
      Vault server instance from within a Docker container, in a completely automated manner.  You no longer have to
      manually configure and run a Vault server to use the test suite!
    * The tests are now going against a regular Vault server, rather than one running in "dev mode".  Therefore,
      they are now able to use HTTPS connections rather than plain HTTP.
    * Upgrades tests to use Java 8 (although the library itself still targets Java 7).
  * **Misc / quality-of-life**:
    * Includes the REST response body in `VaultException` messages for basic read and write operations.
    * Makes numerous classes implement `Serializable`.
    * Upgrades the project to Gradle 4.0.

* **2.0.0**: This is breaking-change release, with numerous deprecated items cleaned up.
  * Adds support for authentication via the AppRole auth backend.
  * Adds support for renewing secret leases.
  * Removes the `com.bettercloud.vault.api.Sys` class, deprecated in the 1.2.0 release.
  * Removes the `com.bettercloud.vault.api.Auth.loginByUsernamePassword` method, deprecated in the 1.2.0 release.
  * Removes the fields `leaseId`, `leaseDuration`, and `renewable` from the `VaultResponse` base class, instead
    including them only in the subclasses for specific response types where they are found.
  * Changes the `com.bettercloud.vault.response.AuthReponse` class field `authLeaseDuration` from type `int` to `long`.
  * Refactors and removes various deprecated `private` methods, with no change to the exposed API.

* **1.2.0**: This is a substantial release, with numerous additions.  It's a minor version number only because there
             should be no breaking changes.  The changes include the following:
  * Switches from Vault 0.5.x to 0.6.x for automated tests.
  * Adds a field to `VaultException` for capturing the HTTP response code (if any) from Vault.
  * Updates the Gradle build, so that you no longer need empty placeholder values for certain variables elsewhere
    in your environment.
  * Updates integration test suite to account for breaking changes in Vault 0.6.x (e.g. you can no longer use
    a token that was obtained from one of the authentication backends to perform tasks such as creating and deleting
    roles, etc).
  * Deprecates the App ID authentication backend, and adds a new version of the Userpass authentication backend that
    doesn't require a path prefix.  Adds support for the GitHub authentication backend.
  * If the `VAULT_TOKEN` environment parameter is not set, then the driver will now check for a file named `.vault-token`
    in the executing user's home directory, and try to read a token value from that.
  * Deprecates the `com.bettercloud.vault.api.Sys` class, moving the debug-related methods into their own
    specific `com.bettercloud.vault.api.Debug` class instead.
  * Implements some of the lease related endpoints (i.e. revoke, revoke-prefix, revoke-force).
  * Supports PKI backends that are mounted on non-default paths.

* **1.1.1**: Changes the `ttl` argument to `Pki.issue()` from `Integer` to `String`, to fix a bug preventing
             you from specifying the time suffix (e.g. "1h").
* **1.1.0**: Switches from Vault 0.4.x to 0.5.x for automated tests.  Adds support to the Logical
             API wrapper for listing and deleting secrets.  Implements the `/v1/sys/health` health-check
             HTTP API endpoint.  Implements portions of the PKI backend (e.g. creating and deleting roles, issuing
             credentials).  Marks numerous methods as deprecated, to be removed in a future major release.
* **1.0.0**: Drops support for Java 6.  Removes all methods marked as `@Deprecated` in version 0.5.0.  Adds
             support for response metadata (i.e. "lease_id", "renewable", "lease_duration") to all response
             types, rather than just `AuthResponse`.  Changes `leaseDuration` type from `int` to `Long` in
             `AuthResponse`.  Removes `final` declarations on all classes (outside of the JSON package).
             Various bugfixes.  Adds support for auth token self-renewal.  Adds support for writing values
             that return content.
* **0.5.0**: Adds support for supplying SSL certificates, and for toggling whether or not the Vault server's
             SSL certificate will be verified.  Also adds support for "openTimeout" and "readTimeout"
             settings.  Deprecates the "timeout", "sslTimeout", "proxyAddress", "proxyPort", "proxyUsername",
             and "proxyPassword" settings (the proxy settings may return in a future version, but it's too
             misleading to have methods exposed for settings that won't really be supported soon).
* **0.3.0**: Initial public release.  Support for writing and reading secrets, authenticating with the "AppID"
             or "Username & Password" auth backends.  All over-the-wire methods support automatic retry logic.

Development
-----------
Pull requests are welcomed for bugfixes or enhancements that do not alter the external facing class and method
signatures.  For any breaking changes that would alter the contract provided by this driver, please open up an issue
to discuss it first.

All code changes should include unit test and/or integration test coverage as appropriate.  Unit tests are any that
can be run in isolation, with no external dependencies.  Integration tests are those which require a Vault server
instance (at least a Dev Server) up and running.

Unit tests are located under the `src/test` directory, and can be run with the Grade `unitTest` task.

Integration tests are located under the `src/test-integration` directory, and can be run with the Gradle
`integrationTest` task.  See the additional `README.md` file in this directory for more detailed information.

Although this library now includes a `module-info` class for use by Java 9+, the library currently targets
Java 8 compatibility.  Please do not attempt to introduce any features or syntax not compatible with Java 8 (the
Gradle build script should prevent you from doing so without modification).

License
-------
The MIT License (MIT)

Copyright (c) 2016-2019 BetterCloud
Copyright (c) 2021 Ian Ferguson

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Other Notes
-----------
The Vault server system itself is a product of HashiCorp, a completely separate organization.

This client driver adapts JSON parsing code from Ralf Sternberg's excellent
[minimal-json](https://github.com/ralfstx/minimal-json) library, likewise available under the MIT License.  Package
names have all been changed, to prevent any conflicts should you happen to be using a different version of that
library elsewhere in your project dependencies.

### Why did you fork BetterCloud/vault-java-driver

BetterCloud's [vault-java-driver](https://github.com/BetterCloud/vault-java-driver) is one of the
most commonly used Java clients for Hashicorp, but has had no activity or releases since December 2019.

The lack of [X-Vault-Request header support](https://github.com/BetterCloud/vault-java-driver/pull/229) prevents it from
being used with Vault Agent instances configured with the [`require_request_header` anti-SSRF setting enabled](https://www.vaultproject.io/api#the-x-vault-request-header).

