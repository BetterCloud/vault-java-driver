package com.bettercloud.vault.json;

public final class JsonParser {

    public void foo(final String jsonString) throws JsonException {
        int index = indexNonWhitespace(jsonString);
        if (index < 0) {
            throw new JsonException("Invalid JSON object");
        }
        if (jsonString.charAt(index) != '{') {
            throw new JsonException("Invalid JSON object, must start with a '{' character");
        }
        while (true) {

        }

    }

    public void parseObject(final String jsonString) throws JsonException {

        // Basic validation

        final String trimmedJsonString = jsonString.trim();
        if (trimmedJsonString.length() < 2) {
            throw new JsonException("Invalid JSON object, must start and end with '{' and '}' characters");
        }
        if (trimmedJsonString.charAt(0) != '{') {
            throw new JsonException("Invalid JSON, must begin with a '{' character");
        }
        if (trimmedJsonString.charAt(trimmedJsonString.length() - 1) != '}') {
            throw new JsonException("Invalid JSON, must end with a '}' character");
        }

        // Parse field key

    }

    private int indexNonWhitespace(final String jsonString) {
        return indexNonWhitespace(jsonString, 0);
    }

    private int indexNonWhitespace(final String jsonString, final int fromIndex) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return -1;
        }
        for (int index = fromIndex; index < jsonString.length(); index++) {
            if (!Character.isWhitespace(jsonString.charAt(index))) {
                return index;
            }
        }
        return -1;
    }



}

