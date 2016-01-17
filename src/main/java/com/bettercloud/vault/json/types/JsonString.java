package com.bettercloud.vault.json.types;

public final class JsonString implements JsonType {

    private String value = "";

    public JsonString() {
    }

    public JsonString(final String value) {
        this.value = value == null ? "" : value;
    }

    public String getValue() {
        return value;
    }
}
