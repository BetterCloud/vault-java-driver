package com.bettercloud.vault.api.mounts;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor(staticName = "of")
public class TimeToLive {
    @Getter private final int ttl;
    @Getter @NonNull private final TimeUnit unit;

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
