package com.bettercloud.vault.api.mounts;

import java.util.concurrent.TimeUnit;

/**
 * <p>A container for Time To Live information sent to mounts endpoints on the Secret Engine backend as REST payload.
 * This class is meant for use with a static <code>TimeToLive.of(int, TimeUnit)</code> method pattern style.  Example
 * usage:</p>
 *
 * <blockquote>
 * <pre>{@code
 * TimeToLive.of(1, TimeUnit.HOURS);
 * }</pre>
 * </blockquote>
 *
 * <p>Note that the only accepted <code>TimeUnit</code> which Vault backend understands are:</p>
 *
 * <ul>
 * <li><code>TimeUnit.SECONDS</code></li>
 * <li><code>TimeUnit.MINUTES</code></li>
 * <li><code>TimeUnit.HOURS</code></li>
 * </ul>
 */
public class TimeToLive {
    private final int ttl;
    private final TimeUnit unit;

    public static TimeToLive of(final int ttl, final TimeUnit unit) {
        return new TimeToLive(ttl, unit);
    }

    private TimeToLive(final int ttl, final TimeUnit unit) {
        if (unit == null) throw new NullPointerException("unit is null");
        this.ttl = ttl;
        this.unit = unit;
    }

    public String toString() {
        return new StringBuilder()
                .append(ttl)
                .append(convertTimeUnit())
                .toString();
    }

    private String convertTimeUnit() {
        if (unit == TimeUnit.SECONDS) {
            return "s";
        } else if (unit == TimeUnit.MINUTES) {
            return "m";
        } else if (unit == TimeUnit.HOURS) {
            return "h";
        } else {
            throw new IllegalArgumentException(unit + " is not a vaild TimeUnit for Vault");
        }
    }
}
