package com.bettercloud.vault.json.types;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonObject implements JsonType {

    private final Map<String, JsonType> fields = new ConcurrentHashMap<>();

    public JsonObject() {
    }

    public JsonObject(final Map<String, JsonType> fields) {
        this.fields.putAll(fields);
    }

    public JsonType get(final String name) {
        return fields.get(name);
    }

    public void set(final String name, final JsonType value) {
        fields.put(name, value);
    }

    public Set<String> fieldNames() {
        return fields.keySet();
    }

}
