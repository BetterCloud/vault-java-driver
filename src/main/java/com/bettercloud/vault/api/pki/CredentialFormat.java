package com.bettercloud.vault.api.pki;

import java.util.List;

/**
 * <p>Possible format options for credentials issued by the PKI backend.</p>
 *
 * <p>See: {@link Pki#issue(String, String, List, List, String, CredentialFormat)}</p>
 */
public enum CredentialFormat {
    PEM,
    DER,
    PEM_BUNDLE;

    public static CredentialFormat fromString(final String text) {
        if (text != null) {
            for (final CredentialFormat format : CredentialFormat.values()) {
                if (text.equalsIgnoreCase(format.toString())) {
                    return format;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
