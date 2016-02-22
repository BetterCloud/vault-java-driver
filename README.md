Vault Java Driver
=================
A zero-dependency Java client for the [Vault](https://www.vaultproject.io/) secrets management solution from
HashiCorp.

This driver strives to implement Vault's full HTTP API, along with supporting functionality such as automatic
retry handling.  It does so without relying on any other external libraries beyond the Java standard library,
and is compatible with Java environments as far back as Java 6.  So it will play nice with all of your projects,
greenfield and legacy alike, without causing conflicts with any other dependency.

Installing the Driver
---------------------
The driver is available from Maven Central, for all modern Java build systems.

Gradle:
```
dependencies {
    compile('com.bettercloud:vault-java-driver:0.5.0')
}
```

Maven:
```
<dependency>
    <groupId>com.bettercloud</groupId>
    <artifactId>vault-java-driver</artifactId>
    <version>0.5.0</version>
</dependency>
```

Initializing a Driver Instance
------------------------------
The `com.bettercloud.vault.VaultConfig` class is used to initialize a driver instance with desired settings.
In the most basic use cases, where you are only supplying a Vault server address and perhaps a root token, there
are convenience constructor methods available:
```
final VaultConfig config = new VaultConfig("http://127.0.0.1:8200", "3c9fd6be-7bc2-9d1f-6fb3-cd746c0fc4e8");

// You may choose not to provide a root token initially, if you plan to use
// the Vault driver to retrieve one programmatically from an auth backend.
final VaultConfig config = new VaultConfig("http://127.0.0.1:8200");
```

To explicitly set additional config parameters (*), you can use a builder pattern style to construct the `VaultConfig`
instance.  Either way, the initialization process will try to populate any unset values by looking to
environment variables.

```
final VaultConfig config =
    new VaultConfig().
        .address("http://127.0.0.1:8200")               // Defaults to "VAULT_ADDR" environment variable
        .token("3c9fd6be-7bc2-9d1f-6fb3-cd746c0fc4e8")  // Defaults to "VAULT_TOKEN" environment variable
        .openTimeout(5)                                 // Defaults to "VAULT_OPEN_TIMEOUT" environment variable
        .readTimeout(30)                                // Defaults to "VAULT_READ_TIMEOUT" environment variable
        .sslPemFile("/path/on/disk.pem")                // Defaults to "VAULT_SSL_CERT" environment variable
                                                        //    See also: "sslPemUTF8()" and "sslPemResource()"
        .sslVerify(false)                               // Defaults to "VAULT_SSL_VERIFY" environment variable
        .build();
```

> NOTES ON SSL CONFIG
>
> If your Vault server uses a SSL certificate, there are three different options for supplying that certificate to the
> Vault driver:
>
> `sslPemFile(path)` - Supply the path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding.
>
> `sslPemResource(path)` - Same as above, but the path references a classpath resource rather than a filesystem path (e.g. if
>                        you've bundled the PEM file into your applications's JAR, WAR, or EAR file).
>
> `sslPemUTF8(contents)` - The string contents extracted from the PEM file.  For Java to parse the certificate properly,
>                        there must be a line-break in between the certificate header and body (see the `VaultConfig`
>                        Javadocs for more detail).
>
> If none of these three methods are called, then `VaultConfig` will by default check for a `VAULT_SSL_CERT` environment
> variable, and if that's set then it will be treated as a filesystem path.
>
> To disable SSL certificate verification altogether, set `sslVerify(false)`.  YOU SHOULD NOT DO THIS IS A REAL
> PRODUCTION SETTING!  However, it can be useful in a development or testing server context.

Once you have initialized a `VaultConfig` object, you can use it to construct an instance of the `Vault` primary
driver class:

```
final Vault vault = new Vault(config);
```

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

The driver DSL also allows you to specify retry logic, by chaining the `withRetries()` ahead of accessing the endpoint
implementation:

```
// Retry up to 5 times if failures occur, waiting 1000 milliseconds in between each retry attempt.
final LogicalResponse response = vault.logical()
                                   .withRetries(5, 1000)
                                   .read("secret/hello");
```

Reference
---------
Full [Javadoc documentation](http://bettercloud.github.io/vault-java-driver/javadoc/).

Version History
---------------
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
`integrationTest` task.  See the additional `README.md` file in this directory for more detailed information on the
Vault server setup steps required to run the integration test suite.

License
-------
The MIT License (MIT)

Copyright (c) 2016 BetterCloud

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

