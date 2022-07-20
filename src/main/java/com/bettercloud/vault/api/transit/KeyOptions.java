package com.bettercloud.vault.api.transit;

public class KeyOptions {

    private Boolean convergentEncryption;

    private Boolean derived;

    private Boolean exportable;

    private Boolean allowPlaintextBackup;

    private String type;

    private Integer autoRotatePeriod;

    //read only
    private Boolean deletionAllowed;

    private String name;

    private Integer minDecryptionVersion;

    private Integer minEncryptionVersion;

    private Boolean supportsEncryption;

    private Boolean supportsDecryption;

    private Boolean supportsDerivation;

    private Boolean supportsSigning;

    public KeyOptions() {
    }

    // from response
    public KeyOptions(Boolean deletionAllowed, String name, Integer minDecryptionVersion,
            Integer minEncryptionVersion, Boolean supportsEncryption, Boolean supportsDecryption,
            Boolean supportsDerivation, Boolean supportsSigning) {
        this.deletionAllowed = deletionAllowed;
        this.name = name;
        this.minDecryptionVersion = minDecryptionVersion;
        this.minEncryptionVersion = minEncryptionVersion;
        this.supportsEncryption = supportsEncryption;
        this.supportsDecryption = supportsDecryption;
        this.supportsDerivation = supportsDerivation;
        this.supportsSigning = supportsSigning;
    }

    public Boolean getConvergentEncryption() {
        return convergentEncryption;
    }

    public Boolean getDerived() {
        return derived;
    }

    public Boolean getExportable() {
        return exportable;
    }

    public Boolean getAllowPlaintextBackup() {
        return allowPlaintextBackup;
    }

    public String getType() {
        return type;
    }

    public Integer getAutoRotatePeriod() {
        return autoRotatePeriod;
    }

    public Boolean getDeletionAllowed() {
        return deletionAllowed;
    }

    public String getName() {
        return name;
    }

    public Integer getMinDecryptionVersion() {
        return minDecryptionVersion;
    }

    public Integer getMinEncryptionVersion() {
        return minEncryptionVersion;
    }

    public Boolean getSupportsEncryption() {
        return supportsEncryption;
    }

    public Boolean getSupportsDecryption() {
        return supportsDecryption;
    }

    public Boolean getSupportsDerivation() {
        return supportsDerivation;
    }

    public Boolean getSupportsSigning() {
        return supportsSigning;
    }

    public KeyOptions convergentEncryption(Boolean convergentEncryption) {
        this.convergentEncryption = convergentEncryption;
        return this;
    }

    public KeyOptions derived(Boolean derived) {
        this.derived = derived;
        return this;
    }

    public KeyOptions exportable(Boolean exportable) {
        this.exportable = exportable;
        return this;
    }

    public KeyOptions allowPlaintextBackup(Boolean allowPlaintextBackup) {
        this.allowPlaintextBackup = allowPlaintextBackup;
        return this;
    }

    public KeyOptions type(String type) {
        this.type = type;
        return this;
    }

    public KeyOptions autoRotatePeriod(Integer autoRotatePeriod) {
        this.autoRotatePeriod = autoRotatePeriod;
        return this;
    }
}
