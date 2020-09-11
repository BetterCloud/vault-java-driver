package com.bettercloud.vault.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WriteOptions {

    public static final String CHECK_AND_SET_KEY = "cas";

    private final Map<String, Object> options = new HashMap<>();

    public WriteOptions checkAndSet(Long version) {
        return setOption(CHECK_AND_SET_KEY, version);
    }

    public WriteOptions setOption(String name, Object value) {
        options.put(name, value);
        return this;
    }

    public WriteOptions build() {
        return this;
    }

    public Map<String, Object> getOptionsMap() {
        return Collections.unmodifiableMap(options);
    }

    public boolean isEmpty() {
        return options.isEmpty();
    }

}
