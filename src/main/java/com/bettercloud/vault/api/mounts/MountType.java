package com.bettercloud.vault.api.mounts;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * <p>A representation of different available secret engine mount points</p>
 */
public enum MountType {
    AWS("aws"),

    CONSUL("consul"),

    CUBBYHOLE("cubbyhole"),

    DATABASE("database"),

    KEY_VALUE("kv"),

    IDENTITY("identity"),

    NOMAD("nomad"),

    PKI("pki"),

    RABBITMQ("rabbitmq"),

    SSH("ssh"),

    SYSTEM("system"),

    TOTP("totp"),

    TRANSIT("transit");

    private final String value;

    private MountType(final String value) {
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

        return Arrays.asList(MountType.values())
                .stream()
                .filter(new Predicate<MountType>() {
                    public boolean test(MountType type) {
                        return type.value.equals(value);
                    }
                })
                .findFirst()
                .orElse(null);
    }
}
