package com.bettercloud.vault.response;

import com.bettercloud.vault.api.pki.RoleOptions;
import com.bettercloud.vault.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * TODO: Document
 */
public class PkiResponse extends LogicalResponse {

    private RoleOptions roleOptions;

    public PkiResponse(RestResponse restResponse, int retries) {
        super(restResponse, retries);
    }

    public PkiResponse(RestResponse restResponse, int retries, Map<String, String> data) {
        super(restResponse, retries, data);
        roleOptions = buildRoleOptionsFromData(data);

        // TODO: Parse out certifcate info for "/issue" endpoint calls

    }

    /**
     * TODO: Document
     *
     * @param data
     * @return
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
        List<String> returnValue = new ArrayList<>();
        final StringTokenizer tokenizer = new StringTokenizer(input, ",");
        while (tokenizer.hasMoreTokens()) {
            returnValue.add(tokenizer.nextToken());
        }
        return returnValue;
    }

    /**
     * <p>Used to convert a String to an object Long, if possble.</p>
     *
     * @param input
     * @return
     */
    private Long parseLong(final String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
