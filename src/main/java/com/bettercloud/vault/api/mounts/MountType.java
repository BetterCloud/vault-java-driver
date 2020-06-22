package com.bettercloud.vault.api.mounts;

import java.util.Arrays;

/**
 * <p>A representation of different available secret engine mount points</p>
 */
public enum MountType {
    AWS("aws"),

    CONSUL("consul"),

    CUBBYHOLE("cubbyhole"),

    DATABASE("database"),

    KEY_VALUE("kv"),

    KEY_VALUE_V2("kv-v2"),

    IDENTITY("identity"),

    NOMAD("nomad"),

    PKI("pki"),

    RABBITMQ("rabbitmq"),

    SSH("ssh"),

    SYSTEM("system"),

    TOTP("totp"),

    TRANSIT("transit");

    private final String value;

    MountType(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * <p>Get the <code>MountType</code> instance from the provided <code>value</code> string.
     *
     * @param value The mount type value to use to lookup.
     *
     * @return an instance of <code>MountType</code> or <code>null</code>
     */
    public static MountType of(final String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(MountType.values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
