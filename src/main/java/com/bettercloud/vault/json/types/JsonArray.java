package com.bettercloud.vault.json.types;

import com.bettercloud.vault.json.JsonException;

import java.util.ArrayList;
import java.util.List;

public final class JsonArray implements JsonType {

    private final NoNullsList<JsonType> elements = new NoNullsList<>();

}

class NoNullsList<T> {

    private final List<T> elements = new ArrayList<>();

    public void add(final T element) throws JsonException {
        if (element == null) {
            throw new JsonException("Cannot add a raw null to a JsonArray.  Use an object of type JsonNull instead.");
        }
        this.elements.add(element);
    }

}
