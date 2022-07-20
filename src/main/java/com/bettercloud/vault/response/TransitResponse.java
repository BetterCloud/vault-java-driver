package com.bettercloud.vault.response;

import com.bettercloud.vault.api.Logical.logicalOperations;
import com.bettercloud.vault.api.transit.CryptData;
import com.bettercloud.vault.api.transit.KeyOptions;
import com.bettercloud.vault.rest.RestResponse;
import java.util.Map;

public class TransitResponse extends LogicalResponse {

    private KeyOptions keyOptions;

    private CryptData cryptData;


    /**
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     */
    public TransitResponse(RestResponse restResponse, int retries) {
        this(restResponse, retries, logicalOperations.authentication);
    }

    /**
     * @param restResponse The raw HTTP response from Vault.
     * @param retries The number of retry attempts that occurred during the API call (can be zero).
     * @param operation The operation requested.
     */
    public TransitResponse(RestResponse restResponse, int retries, logicalOperations operation) {
        super(restResponse, retries, operation);
        keyOptions = buildKeyOptionsFromData(this.getData());
        cryptData = buildEncryptDataFromData(this.getData());
    }

    public KeyOptions getKeyOptions() {
        return keyOptions;
    }

    public CryptData getCryptData() {
        return cryptData;
    }

    /**
     * <p>Generates a <code>KeyOptions</code> object from the response data returned by Transit
     * backend REST calls, for those calls which do return role data
     * (e.g. <code>getKey(String keyName)</code>).</p>
     *
     * <p>If the response data does not contain key information, then this method will
     * return <code>null</code>.</p>
     *
     * @param data The <code>"data"</code> object from a Vault JSON response, converted into Java key-value pairs.
     * @return A container for role options
     */
    private KeyOptions buildKeyOptionsFromData(final Map<String, String> data) {
        if (data == null) {
            return null;
        }

        final String type = data.get("type");
        final Boolean deletionAllowed = parseBoolean(data.get("deletion_allowed"));
        final Boolean derived = parseBoolean(data.get("derived"));
        final Boolean exportable = parseBoolean(data.get("exportable"));
        final Boolean allowPlaintextBackup = parseBoolean(data.get("allow_plaintext_backup"));
        final Integer minDecryptionVersion = parseInt(data.get("min_decryption_version"));
        final Integer minEncryptionVersion = parseInt(data.get("min_encryption_version"));
        final String name = data.get("name");
        final Boolean supportsEncryption = parseBoolean(data.get("supports_encryption"));
        final Boolean supportsDecryption = parseBoolean(data.get("supports_decryption"));
        final Boolean supportsDerivation = parseBoolean(data.get("supports_derivation"));
        final Boolean supportsSigning = parseBoolean(data.get("supports_signing"));

        if ( type == null && deletionAllowed == null && derived == null && exportable == null
                && allowPlaintextBackup == null && minDecryptionVersion == null
                && minEncryptionVersion == null && name == null && supportsEncryption == null
                && supportsDecryption == null && supportsDerivation == null
                && supportsSigning == null ) {
            return null;
        }
        return new KeyOptions(deletionAllowed, name, minDecryptionVersion, minEncryptionVersion,
                supportsEncryption, supportsDecryption, supportsDerivation, supportsSigning)
                .type(type)
                .derived(derived)
                .exportable(exportable)
                .allowPlaintextBackup(allowPlaintextBackup);
    }

    /**
     * <p>Generates a <code>EncryptData</code> object from the response data returned by Transit
     * backend REST calls, for those calls which do return role data
     * (e.g. <code>encryptData(String keyName, EncryptOptions options)</code>).</p>
     *
     * @param data The <code>"data"</code> object from a Vault JSON response, converted into Java key-value pairs.
     * @return A container for role options
     */
    private CryptData buildEncryptDataFromData(final Map<String, String> data) {
        if (data == null) {
            return null;
        }

        final String ciphertext = data.get("ciphertext");
        final String plaintext = data.get("plaintext");
        final Integer keyVersion = parseInt(data.get("key_version"));

        if ( ciphertext == null && plaintext == null && keyVersion == null) {
            return null;
        }
        return new CryptData()
                .ciphertext(ciphertext)
                .plaintext(plaintext)
                .keyVersion(keyVersion);
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

    private Integer parseInt(final String input) {
        if (input == null) {
            return null;
        } else {
            return Integer.parseInt(input);
        }
    }

}
