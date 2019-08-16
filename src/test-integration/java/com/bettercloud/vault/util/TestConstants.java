package com.bettercloud.vault.util;

import org.testcontainers.containers.Network;

import java.io.File;

/**
 * Various constants used throughout the integration test suite, but primarily by {@link VaultContainer}
 * and {@link SSLUtils}.  Mostly username/password credentials and other Vault configuration values, and
 * path locations for SSL artifacts.
 */
interface TestConstants {

    String POSTGRES_PASSWORD = "superpassword1";
    String POSTGRES_USER = "superuser1";

    String APP_ID = "fake_app";
    String USER_ID = "fake_user";
    String PASSWORD = "fake_password";
    int MAX_RETRIES = 5;
    int RETRY_MILLIS = 1000;

    String CURRENT_WORKING_DIRECTORY = System.getProperty("user.dir");
    String SSL_DIRECTORY = CURRENT_WORKING_DIRECTORY + File.separator + "ssl";
    String CERT_PEMFILE = SSL_DIRECTORY + File.separator + "root-cert.pem";

    String CLIENT_CERT_PEMFILE = SSL_DIRECTORY + File.separator + "client-cert.pem";
    String CLIENT_PRIVATE_KEY_PEMFILE = SSL_DIRECTORY + File.separator + "client-privatekey.pem";
    String CLIENT_KEYSTORE = SSL_DIRECTORY + File.separator + "keystore.jks";
    String CLIENT_TRUSTSTORE = SSL_DIRECTORY + File.separator + "truststore.jks";

    String CONTAINER_STARTUP_SCRIPT = "/vault/config/startup.sh";
    String CONTAINER_CONFIG_FILE = "/vault/config/config.json";
    String CONTAINER_OPENSSL_CONFIG_FILE = "/vault/config/ssl/libressl.conf";
    String CONTAINER_SSL_DIRECTORY = "/vault/config/ssl";
    String CONTAINER_CERT_PEMFILE = CONTAINER_SSL_DIRECTORY + "/vault-cert.pem";
    String CONTAINER_CLIENT_CERT_PEMFILE = CONTAINER_SSL_DIRECTORY + "/client-cert.pem";

    String AGENT_CONFIG_FILE = "/home/vault/agent.hcl";
    String APPROLE_POLICY_FILE = "/home/vault/approlePolicy.hcl";

    Network CONTAINER_NETWORK = Network.newNetwork();
}
