Intro
=====
Unit tests, which do not rely on a Vault server being available, are separated from
these integration tests, which do require a Vault instance.

Configuring a Vault Server
==========================
It's not necessary to have a production-grade Vault server.  To run these tests, you
can simply run a [Dev Server](https://www.vaultproject.io/intro/getting-started/dev-server.html)
process:

```
$ vault server -dev`

==> WARNING: Dev mode is enabled!

In this mode, Vault is completely in-memory and unsealed.
Vault is configured to only have a single unseal key. The root
token has already been authenticated with the CLI, so you can
immediately begin using the Vault CLI.

The only step you need to take is to set the following
environment variables:

    set VAULT_ADDR=http://127.0.0.1:8200

The unseal key and root token are reproduced below in case you
want to seal/unseal the Vault or play with authentication.

Unseal Key: 642e33b1c397c292743df56da6129a25df6a6349934931f55a2baac34a6e2c80
Root Token: 764cf317-d3b9-3d52-dc7d-e4f0198f6a8c

...
```

Some of the integration tests verify that an authentication  token can be retrieved
from various auth backends (e.g. [App Id](https://www.vaultproject.io/docs/auth/app-id.html),
[Username & Password](https://www.vaultproject.io/docs/auth/userpass.html), etc).
So prior to running these tests, you will need to run some Vault CLI commands to
enable auth backends and populate user and app data:

```
vault auth-enable app-id
vault write auth/app-id/map/app-id/fake_app value=root display_name=fake_app
vault write auth/app-id/map/user-id/fake_user value=fake_app

vault auth-enable userpass
vault write auth/userpass/users/fake_user password=fake_password policies=root

vault mount -path=pki pki
```

Configuring and Running the Integration Tests
=============================================
The Gradle `integrationTest` task is used to execute the integration test suite.
When running this Gradle task, you need to pass several JVM options so that Gradle
will make them available to the tests:

* `VAULT_ADDR`: The connection URL for the Vault server.  The Dev Server displays
  this when the server starts up (e.g. `http://127.0.0.1:8200`) in the example above.
* `VAULT_TOKEN`: The root token, to enable Vault API calls.  The Dev Server also
  displays this at startup (e.g. `764cf317-d3b9-3d52-dc7d-e4f0198f6a8c` in the
  example above).
* `VAULT_APP_ID`: An application ID that has been created in the Vault server,
  for testing the App Id auth backend.  This can be whatever you populate (e.g.
  `fake_app` in the example CLI command above).
* `VAULT_USER_ID`: A user ID that has been created in the Vault server, for testing
  the Username and Password auth backend.  This can be whatever you populate (e.g.
  `fake_user` in the example CLI command above).
* `VAULT_PASSWORD`: The password corresponding to the above user (e.g. `fake_password`
  in the CLI command above).

Example Gradle invocation:

`$ gradle integrationTest -DVAULT_ADDR=http://127.0.0.1:8200 -DVAULT_TOKEN=764cf317-d3b9-3d52-dc7d-e4f0198f6a8c -DVAULT_APP_ID=fake_app -DVAULT_USER_ID=fake_user -DVAULT_PASSWORD=fake_password`
