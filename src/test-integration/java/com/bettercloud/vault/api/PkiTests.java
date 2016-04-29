package com.bettercloud.vault.api;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * TODO: Document
 */
public class PkiTests {

    @Before
    public void setup() {
        // TODO: Delete
    }

    @Test
    public void testCreateRole_Defaults() throws VaultException {
        final String token = authenticate();
        final String address = System.getProperty("VAULT_ADDR");
        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);

        vault.pki().createOrUpdateRole("testRole", null);
        final LogicalResponse response = vault.pki().getRole("testRole");
        compareRoleOptionsToResponseData(new Pki.RoleOptions(), response.getData());
    }

    @Test
    public void testCreateRole_WithOptions() throws VaultException {
        final String token = authenticate();
        final String address = System.getProperty("VAULT_ADDR");
        final VaultConfig config = new VaultConfig(address, token);
        final Vault vault = new Vault(config);

        final Pki.RoleOptions options = new Pki.RoleOptions().allowAnyName(true);
        vault.pki().createOrUpdateRole("testRole", options);
        final LogicalResponse response = vault.pki().getRole("testRole");
        compareRoleOptionsToResponseData(options, response.getData());
    }

    private String authenticate() throws VaultException {
        final String address = System.getProperty("VAULT_ADDR");
        final String userId = System.getProperty("VAULT_USER_ID");
        final String password = System.getProperty("VAULT_PASSWORD");

        assertNotNull(address);
        assertNotNull(userId);
        assertNotNull(password);

        final String path = "userpass/login/" + userId;
        final VaultConfig config = new VaultConfig(address);
        final Vault vault = new Vault(config);

        final String token = vault.auth().loginByUsernamePassword(path, password).getAuthClientToken();
        assertNotNull(token);
        assertNotSame("", token.trim());
        return token;
    }

    private void compareRoleOptionsToResponseData(final Pki.RoleOptions options, final Map<String, String> data) {
        compareRoleOptionField(options.getAllowAnyName(), data.get("allow_any_name"), "false");
        compareRoleOptionField(options.getAllowBareDomains(), data.get("allow_bare_domains"), "false");
        compareRoleOptionField(options.getAllowIpSans(), data.get("allow_ip_sans"), "true");
        compareRoleOptionField(options.getAllowLocalhost(), data.get("allow_localhost"), "true");
        compareRoleOptionField(options.getAllowSubdomains(), data.get("allow_subdomains"), "false");
        compareRoleOptionField(options.getAllowedDomains(), data.get("allowed_domains"), "");
        compareRoleOptionField(options.getClientFlag(), data.get("client_flag"), "true");
        compareRoleOptionField(options.getCodeSigningFlag(), data.get("code_signing_flag"), "false");
        compareRoleOptionField(options.getEmailProtectionFlag(), data.get("email_protection_flag"), "false");
        compareRoleOptionField(options.getEnforceHostnames(), data.get("email_protection_flag"), "false");
        compareRoleOptionField(options.getKeyBits(), data.get("key_bits"), "2048");
        compareRoleOptionField(options.getKeyType(), data.get("key_type"), "rsa");
        compareRoleOptionField(options.getMaxTtl(), data.get("max_ttl"), "(system default)");
        compareRoleOptionField(options.getServerFlag(), data.get("server_flag"), "true");
        compareRoleOptionField(options.getTtl(), data.get("ttl"), "(system default)");
        compareRoleOptionField(options.getUseCsrCommonName(), data.get("use_csr_common_name"), "true");
    }

    private void compareRoleOptionField(final Object option, final Object data, final Object expectedDefault) {
        assertTrue( (option == null && data == null) || (option == null && data.equals(expectedDefault)) || option.toString().equals(data) );
    }

}
