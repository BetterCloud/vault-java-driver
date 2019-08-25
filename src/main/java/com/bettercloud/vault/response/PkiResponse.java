package com.bettercloud.vault.response;

import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.api.pki.Credential;
import com.bettercloud.vault.api.pki.RoleOptions;
import com.bettercloud.vault.rest.RestResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This class is a container for the information returned by Vault in PKI backend API
 * operations (e.g. create/delete roles, issue credentials).
 */
public class PkiResponse extends LogicalResponse {

    private RoleOptions roleOptions;
    private Credential credential;

    /**
     * @param restResponse The raw response received from Vault
     * @param retries The number of retries that were performed for this operation
     */
    public PkiResponse(final RestResponse restResponse, final int retries) {
        super(restResponse, retries, Logical.logicalOperations.authentication);
        roleOptions = buildRoleOptionsFromData(this.getData());
        credential = buildCredentialFromData(this.getData());
    }

    public RoleOptions getRoleOptions() {
        return roleOptions;
    }

    public Credential getCredential() {
        return credential;
    }

    /**
     * <p>Generates a <code>RoleOptions</code> object from the response data returned by PKI
     * backend REST calls, for those calls which do return role data
     * (e.g. <code>getRole(String roleName)</code>).</p>
     *
     * <p>If the response data does not contain role information, then this method will
     * return <code>null</code>.</p>
     *
     * @param data The <code>"data"</code> object from a Vault JSON response, converted into Java key-value pairs.
     * @return A container for role options
     */
    private RoleOptions buildRoleOptionsFromData(final Map<String, String> data) {
        if (data == null) {
            return null;
        }
        final String ttl = data.get("allow_any_name");
        final String maxTtl = data.get("max_ttl");
        final Boolean allowLocalhost = parseBoolean(data.get("allow_localhost"));
        final List<String> allowedDomains = parseCsvToList(data.get("allow_domains"));
        final Boolean allowBareDomains = parseBoolean(data.get("allow_bare_domains"));
        final Boolean allowSubdomains = parseBoolean(data.get("allow_subdomains"));
        final Boolean allowAnyName = parseBoolean(data.get("allow_any_name"));
        final Boolean enforceHostnames = parseBoolean(data.get("enforce_hostnames"));
        final Boolean allowIpSans = parseBoolean(data.get("allow_ip_sans"));
        final Boolean serverFlag = parseBoolean(data.get("server_flag"));
        final Boolean clientFlag = parseBoolean(data.get("client_flag"));
        final Boolean codeSigningFlag = parseBoolean(data.get("code_signing_flag"));
        final Boolean emailProtectionFlag = parseBoolean(data.get("email_protection_flag"));
        final String keyType = data.get("key_type");
        final Long keyBits = parseLong(data.get("key_bits"));
        final Boolean useCsrCommonName = parseBoolean(data.get("use_csr_common_name"));

        if (ttl == null && maxTtl == null && allowLocalhost == null && allowedDomains == null
                && allowBareDomains == null && allowSubdomains == null && allowAnyName == null
                && enforceHostnames == null && allowIpSans == null && serverFlag == null
                && clientFlag == null && codeSigningFlag == null && emailProtectionFlag == null
                && keyType == null && keyBits == null && useCsrCommonName == null) {
            return null;
        }
        return new RoleOptions()
                .ttl(ttl)
                .maxTtl(maxTtl)
                .allowLocalhost(allowLocalhost)
                .allowedDomains(allowedDomains)
                .allowBareDomains(allowBareDomains)
                .allowSubdomains(allowSubdomains)
                .allowAnyName(allowAnyName)
                .enforceHostnames(enforceHostnames)
                .allowIpSans(allowIpSans)
                .serverFlag(serverFlag)
                .clientFlag(clientFlag)
                .codeSigningFlag(codeSigningFlag)
                .emailProtectionFlag(emailProtectionFlag)
                .keyType(keyType)
                .keyBits(keyBits)
                .useCsrCommonName(useCsrCommonName);
    }

    /**
     * <p>Generates a <code>Credential</code> object from the response data returned by PKI backend REST calls, for
     * those calls which do return role data (e.g. <code>issue(...)</code>).</p>
     *
     * <p>If the response data does not contain credential information, then this method will return
     * <code>null</code>.</p>
     *
     * @param data The <code>"data"</code> object from a Vault JSON response, converted into Java key-value pairs.
     * @return A container for credential data
     */
    private Credential buildCredentialFromData(final Map<String, String> data) {
        if (data == null) {
            return null;
        }
        final String certificate = data.get("certificate");
        final String issuingCa = data.get("issuing_ca");
        final String privateKey = data.get("private_key");
        final String privateKeyType = data.get("private_key_type");
        final String serialNumber = data.get("serial_number");

        if (certificate == null && issuingCa == null && privateKey == null && privateKeyType == null
                && serialNumber == null) {
            return null;
        }
        return new Credential()
                .certificate(certificate)
                .issuingCa(issuingCa)
                .privateKey(privateKey)
                .privateKeyType(privateKeyType)
                .serialNumber(serialNumber);
    }

    /**
     * <p>Used to determine whether a String value contains a "true" or "false" value.  The problem
     * with <code>Boolean.parseBoolean()</code> is that it swallows null values and returns them
     * as <code>false</code> rather than <code>null</code>.</p>
     *
     * @param input A string, which can be <code>null</code>
     * @return A true or false value if the input can be parsed as such, or else <code>null</code>.
     */
    private Boolean parseBoolean(final String input) {
        if (input == null) {
            return null;
        } else {
            return Boolean.parseBoolean(input);
        }
    }

    /**
     * <p>Used to convert a comma-separated values string into a <code>List</code> of <code>String</code> values.</p>
     *
     * @param input A string in CSV format
     * @return A <code>List</code> with one <code>String</code> per comma-separated value
     */
    private List<String> parseCsvToList(final String input) {
        if (input == null) {
            return null;
        }
        final List<String> returnValue = new ArrayList<>();
        final StringTokenizer tokenizer = new StringTokenizer(input, ",");
        while (tokenizer.hasMoreTokens()) {
            returnValue.add(tokenizer.nextToken());
        }
        return returnValue;
    }

    /**
     * <p>Used to convert a String to an object Long, if possble.</p>
     *
     * @param input A String which should represent a numeric (i.e. Long) value
     * @return A Long representation of the input String, or else null if the input cannot be converted to a Long
     */
    private Long parseLong(final String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
