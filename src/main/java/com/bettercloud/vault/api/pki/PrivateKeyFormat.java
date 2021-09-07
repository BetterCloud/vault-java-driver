package com.bettercloud.vault.api.pki;

import java.util.List;

/**
 * <p>Possible format options for private key issued by the PKI backend</p>
 *
 * <p>See: {@link Pki#issue(String, String, List, List, String, PrivateKeyFormat)}</p>
 */
public enum PrivateKeyFormat {
    DER,
    PEM,
    PKCS8;

    public static PrivateKeyFormat fromString(final String text) {
        if (text != null) {
            for (final PrivateKeyFormat format : PrivateKeyFormat.values()) {
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
