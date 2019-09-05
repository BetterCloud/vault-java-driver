package com.bettercloud.vault.api.pki;

import java.util.List;

/**
 * Value class for options to be passed to the PKI issue API.
 */
public class IssueOptions {

    private List<String> altNames;
    private List<String> ipSans;
    private List<String> uriSans;
    private List<String> otherSans;
    private String ttl;
    private CredentialFormat credentialFormat;
    private PrivateKeyFormat privateKeyFormat;
    private Boolean excludeCnFromSans;
    private String csr;

    public IssueOptions altNames(List<String> altNames) {
        this.altNames = altNames;
        return this;
    }

    public IssueOptions ipSans(List<String> ipSans) {
        this.ipSans = ipSans;
        return this;
    }

    public IssueOptions uriSans(List<String> uriSans) {
        this.uriSans = uriSans;
        return this;
    }

    public IssueOptions otherSans(List<String> otherSans) {
        this.otherSans = otherSans;
        return this;
    }

    public IssueOptions ttl(String ttl) {
        this.ttl = ttl;
        return this;
    }

    public IssueOptions credentialFormat(CredentialFormat format) {
        this.credentialFormat = format;
        return this;
    }

    public IssueOptions privateKeyFormat(PrivateKeyFormat privateKeyFormat) {
        this.privateKeyFormat = privateKeyFormat;
        return this;
    }

    public IssueOptions excludeCnFromSans(Boolean excludeCnFromSans) {
        this.excludeCnFromSans = excludeCnFromSans;
        return this;
    }

    public IssueOptions csr(String csr) {
        this.csr = csr;
        return this;
    }

    public List<String> getAltNames() {
        return altNames;
    }

    public List<String> getIpSans() { return ipSans; }

    public List<String> getUriSans() {
        return uriSans;
    }

    public List<String> getOtherSans() {
        return otherSans;
    }

    public String getTtl() {
        return ttl;
    }

    public CredentialFormat getCredentialFormat() { return credentialFormat; }

    public PrivateKeyFormat getPrivateKeyFormat() {
        return privateKeyFormat;
    }

    public Boolean isExcludeCnFromSans() {
        return excludeCnFromSans;
    }

    public String getCsr() {
        return csr;
    }
}
