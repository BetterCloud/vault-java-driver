package com.bettercloud.vault.api.pki;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Document
 */
public class RoleOptions {

    private String ttl;
    private String maxTtl;
    private Boolean allowLocalhost;
    private List<String> allowedDomains;
    private Boolean allowBareDomains;
    private Boolean allowSubdomains;
    private Boolean allowAnyName;
    private Boolean enforceHostnames;
    private Boolean allowIpSans;
    private Boolean serverFlag;
    private Boolean clientFlag;
    private Boolean codeSigningFlag;
    private Boolean emailProtectionFlag;
    private String keyType;
    private Long keyBits;
    private Boolean useCsrCommonName;

    /**
     * TODO: Document
     */
    public RoleOptions ttl(final String ttl) {
        this.ttl = ttl;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions maxTtl(final String maxTtl) {
        this.maxTtl = maxTtl;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions allowLocalhost(final Boolean allowLocalhost) {
        this.allowLocalhost = allowLocalhost;
        return this;
    }

    /**
     * TODO: Document
     *
     * @param allowedDomains
     * @return
     */
    public RoleOptions allowedDomains(final List<String> allowedDomains) {
        if (allowedDomains != null) {
            this.allowedDomains = new ArrayList<>();
            this.allowedDomains.addAll(allowedDomains);
        }
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions allowBareDomains(final Boolean allowBareDomains) {
        this.allowBareDomains = allowBareDomains;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions allowSubdomains(final Boolean allowSubdomains) {
        this.allowSubdomains = allowSubdomains;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions allowAnyName(final Boolean allowAnyName) {
        this.allowAnyName = allowAnyName;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions enforceHostnames(final Boolean enforceHostnames) {
        this.enforceHostnames = enforceHostnames;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions allowIpSans(final Boolean allowIpSans) {
        this.allowIpSans = allowIpSans;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions serverFlag(final Boolean serverFlag) {
        this.serverFlag = serverFlag;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions clientFlag(final Boolean clientFlag) {
        this.clientFlag = clientFlag;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions codeSigningFlag(final Boolean codeSigningFlag) {
        this.codeSigningFlag = codeSigningFlag;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions emailProtectionFlag(final Boolean emailProtectionFlag) {
        this.emailProtectionFlag = emailProtectionFlag;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions keyType(final String keyType) {
        this.keyType = keyType;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions keyBits(final Long keyBits) {
        this.keyBits = keyBits;
        return this;
    }
    /**
     * TODO: Document
     */
    public RoleOptions useCsrCommonName(final Boolean useCsrCommonName) {
        this.useCsrCommonName = useCsrCommonName;
        return this;
    }

    public String getTtl() {
        return ttl;
    }

    public String getMaxTtl() {
        return maxTtl;
    }

    public Boolean getAllowLocalhost() {
        return allowLocalhost;
    }

    public List<String> getAllowedDomains() {
        if (allowedDomains == null) {
            return null;
        } else {
            final List<String> clone = new ArrayList<>();
            clone.addAll(allowedDomains);
            return allowedDomains;
        }
    }

    public Boolean getAllowBareDomains() {
        return allowBareDomains;
    }

    public Boolean getAllowSubdomains() {
        return allowSubdomains;
    }

    public Boolean getAllowAnyName() {
        return allowAnyName;
    }

    public Boolean getEnforceHostnames() {
        return enforceHostnames;
    }

    public Boolean getAllowIpSans() {
        return allowIpSans;
    }

    public Boolean getServerFlag() {
        return serverFlag;
    }

    public Boolean getClientFlag() {
        return clientFlag;
    }

    public Boolean getCodeSigningFlag() {
        return codeSigningFlag;
    }

    public Boolean getEmailProtectionFlag() {
        return emailProtectionFlag;
    }

    public String getKeyType() {
        return keyType;
    }

    public Long getKeyBits() {
        return keyBits;
    }

    public Boolean getUseCsrCommonName() {
        return useCsrCommonName;
    }

}
