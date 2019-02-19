package com.bettercloud.vault.api;

import com.bettercloud.vault.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class LogicalUtilities {

    /**
     * Convenience method to split a Vault path into its path segments.
     *
     * @param path The Vault path to check or mutate, based on the operation.
     * @return The path potentially mutated, based on the operation
     */
    private static List<String> getPathSegments(final String path) {
        final List<String> segments = new ArrayList<>();
        final StringTokenizer tokenizer = new StringTokenizer(path, "/");
        while (tokenizer.hasMoreTokens()) {
            segments.add(tokenizer.nextToken());
        }
        return segments;
    }

    /**
     * Injects the supplied qualifier (either "data" or "metadata") into the second-from-the-root segment position, for a Vault
     * path to be converted for use with a Version 2 secret engine.
     *
     * @param segments The Vault path split into segments.
     * @param qualifier The String to add to the path, based on the operation.
     * @return The final path with the needed qualifier.
     */
    public static String addQualifierToPath(final List<String> segments, final String qualifier) {
        final StringBuilder adjustedPath = new StringBuilder(segments.get(0)).append('/').append(qualifier).append('/');
        for (int index = 1; index < segments.size(); index++) {
            adjustedPath.append(segments.get(index));
            if (index + 1 < segments.size()) {
                adjustedPath.append('/');
            }
        }
        return adjustedPath.toString();
    }

    /**
     * In version 1 style secret engines, the same path is used for all CRUD operations on a secret.  In version 2 though, the
     * path varies depending on the operation being performed.  When reading or writing a secret, you must inject the path
     * segment "data" right after the lowest-level path segment.
     *
     * @param path      The Vault path to check or mutate, based on the operation.
     * @param operation The operation being performed, e.g. readV2 or writeV1.
     * @return The Vault path mutated based on the operation.
     */
    public static String adjustPathForReadOrWrite(final String path, final Logical.logicalOperations operation) {
        final List<String> pathSegments = getPathSegments(path);
        if (operation.equals(Logical.logicalOperations.readV2) || operation.equals(Logical.logicalOperations.writeV2)) {
            // Version 2
            final StringBuilder adjustedPath = new StringBuilder(addQualifierToPath(pathSegments, "data"));
            if (path.endsWith("/")) {
                adjustedPath.append("/");
            }
            return adjustedPath.toString();
        } else {
            // Version 1
            return path;
        }
    }

    /**
     * In version 1 style secret engines, the same path is used for all CRUD operations on a secret.  In version 2 though, the
     * path varies depending on the operation being performed.  When listing secrets available beneath a path, you must inject the
     * path segment "metadata" right after the lowest-level path segment.
     *
     * @param path      The Vault path to check or mutate, based on the operation.
     * @param operation The operation being performed, e.g. readV2 or writeV1.
     * @return The Vault path mutated based on the operation.
     */
    public static String adjustPathForList(final String path, final Logical.logicalOperations operation) {
        final List<String> pathSegments = getPathSegments(path);
        final StringBuilder adjustedPath = new StringBuilder();
        if (operation.equals(Logical.logicalOperations.listV2)) {
            // Version 2
            adjustedPath.append(addQualifierToPath(pathSegments, "metadata"));
            if (path.endsWith("/")) {
                adjustedPath.append("/");
            }
        } else {
            // Version 1
            adjustedPath.append(path);
        }
        adjustedPath.append("?list=true");
        return adjustedPath.toString();
    }

    /**
     * In version 1 style secret engines, the same path is used for all CRUD operations on a secret.  In version 2 though, the
     * path varies depending on the operation being performed.  When deleting secrets, you must inject the  path segment "metadata"
     * right after the lowest-level path segment.
     *
     * @param path      The Vault path to check or mutate, based on the operation.
     * @param operation The operation being performed, e.g. readV2 or writeV1.
     *
     * @return The modified path
     */
    public static String adjustPathForDelete(final String path, final Logical.logicalOperations operation) {
        final List<String> pathSegments = getPathSegments(path);
        if (operation.equals(Logical.logicalOperations.deleteV2)) {
            final StringBuilder adjustedPath = new StringBuilder(addQualifierToPath(pathSegments, "metadata"));
            if (path.endsWith("/")) {
                adjustedPath.append("/");
            }
            return adjustedPath.toString();
        } else {
            return path;
        }
    }

    /**
     * When deleting secret versions, you must inject the path segment "delete" right after the lowest-level path segment.
     *
     * @param path The Vault path to check or mutate, based on the operation.
     *
     * @return The modified path
     */
    public static String adjustPathForVersionDelete(final String path) {
        final List<String> pathSegments = getPathSegments(path);
        final StringBuilder adjustedPath = new StringBuilder(addQualifierToPath(pathSegments, "delete"));
        if (path.endsWith("/")) {
            adjustedPath.append("/");
        }
        return adjustedPath.toString();
    }

    /**
     * When undeleting secret versions, you must inject the path segment "undelete" right after the lowest-level path segment.
     *
     * @param path The Vault path to check or mutate, based on the operation.
     * @return The path mutated depending on the operation.
     */
    public static String adjustPathForVersionUnDelete(final String path) {
        final List<String> pathSegments = getPathSegments(path);
        final StringBuilder adjustedPath = new StringBuilder(addQualifierToPath(pathSegments, "undelete"));
        if (path.endsWith("/")) {
            adjustedPath.append("/");
        }
        return adjustedPath.toString();
    }

    /**
     * When destroying secret versions, you must inject the path segment "destroy" right after the lowest-level path segment.
     *
     * @param path The Vault path to check or mutate, based on the operation.
     * @return The path mutated depending on the operation.
     */
    public static String adjustPathForVersionDestroy(final String path) {
        final List<String> pathSegments = getPathSegments(path);
        final StringBuilder adjustedPath = new StringBuilder(addQualifierToPath(pathSegments, "destroy"));
        if (path.endsWith("/")) {
            adjustedPath.append("/");
        }
        return adjustedPath.toString();
    }

    /**
     * In version two, when writing a secret, the JSONObject must be nested with "data" as the key.
     *
     * @param operation  The operation being performed, e.g. writeV1, or writeV2.
     * @param jsonObject The jsonObject that is going to be written.
     * @return This jsonObject mutated for the operation.
     */
    public static JsonObject jsonObjectToWriteFromEngineVersion(final Logical.logicalOperations operation, final JsonObject jsonObject) {
        if (operation.equals(Logical.logicalOperations.writeV2)) {
            final JsonObject wrappedJson = new JsonObject();
            wrappedJson.add("data", jsonObject);
            return wrappedJson;
        } else return jsonObject;
    }
}
