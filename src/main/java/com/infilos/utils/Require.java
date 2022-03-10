package com.infilos.utils;

import java.util.Collection;
import java.util.Objects;

public final class Require {
    private Require() {
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void check(boolean condition, String template, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(template, args));
        }
    }

    public static void check(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

    public static <T> T checkNotNull(T object) {
        if (Objects.isNull(object)) {
            throw new IllegalArgumentException("null object");
        }
        
        return object;
    }

    public static <T> T checkNotNull(T object, String message) {
        if (Objects.isNull(object)) {
            throw new IllegalArgumentException(message);
        }

        return object;
    }

    public static <T> T checkNotNull(T object, String template, Object... args) {
        if (Objects.isNull(object)) {
            throw new IllegalArgumentException(String.format(template, args));
        }

        return object;
    }

    public static String checkNotBlank(String string) {
        if (Objects.isNull(string) || string.trim().isEmpty()) {
            throw new IllegalArgumentException("blank string");
        }
        
        return string;
    }

    public static String checkNotBlank(String string, String message) {
        if (Objects.isNull(string) || string.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return string;
    }

    public static String checkNotBlank(String string, String template, Object... args) {
        if (Objects.isNull(string) || string.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format(template, args));
        }

        return string;
    }

    public static <T> Collection<T> checkNotEmpty(Collection<T> collection) {
        if (Objects.isNull(collection) || collection.isEmpty()) {
            throw new IllegalArgumentException("empty collection");
        }
        
        return collection;
    }

    public static <T> Collection<T> checkNotEmpty(Collection<T> collection, String message) {
        if (Objects.isNull(collection) || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return collection;
    }

    public static <T> Collection<T> checkNotEmpty(Collection<T> collection, String template, Object... args) {
        if (Objects.isNull(collection) || collection.isEmpty()) {
            throw new IllegalArgumentException(String.format(template, args));
        }

        return collection;
    }

    public static <T> T[] checkNotEmpty(T[] array) {
        if (Objects.isNull(array) || array.length==0) {
            throw new IllegalArgumentException("empty array");
        }
        
        return array;
    }

    public static <T> T[] checkNotEmpty(T[] array, String message) {
        if (Objects.isNull(array) || array.length==0) {
            throw new IllegalArgumentException(message);
        }

        return array;
    }

    public static <T> T[] checkNotEmpty(T[] array, String template, Object... args) {
        if (Objects.isNull(array) || array.length==0) {
            throw new IllegalArgumentException(String.format(template, args));
        }

        return array;
    }
}
