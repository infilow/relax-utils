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

    public static void checkNotNull(Object object) {
        if (Objects.isNull(object)) {
            throw new IllegalArgumentException("null object");
        }
    }

    public static void checkNotNull(Object object, String message) {
        if (Objects.isNull(object)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkNotNull(Object object, String template, Object... args) {
        if (Objects.isNull(object)) {
            throw new IllegalArgumentException(String.format(template, args));
        }
    }

    public static void checkNotBlank(String string) {
        if (Objects.isNull(string) || string.trim().isEmpty()) {
            throw new IllegalArgumentException("blank string");
        }
    }

    public static void checkNotBlank(String string, String message) {
        if (Objects.isNull(string) || string.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkNotBlank(String string, String template, Object... args) {
        if (Objects.isNull(string) || string.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format(template, args));
        }
    }

    public static void checkNotEmpty(Collection<?> collection) {
        if (Objects.isNull(collection) || collection.isEmpty()) {
            throw new IllegalArgumentException("empty collection");
        }
    }

    public static void checkNotEmpty(Collection<?> collection, String message) {
        if (Objects.isNull(collection) || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkNotEmpty(Collection<?> collection, String template, Object... args) {
        if (Objects.isNull(collection) || collection.isEmpty()) {
            throw new IllegalArgumentException(String.format(template, args));
        }
    }

    public static <T> void checkNotEmpty(T[] array) {
        if (Objects.isNull(array) || array.length==0) {
            throw new IllegalArgumentException("empty array");
        }
    }

    public static <T> void checkNotEmpty(T[] array, String message) {
        if (Objects.isNull(array) || array.length==0) {
            throw new IllegalArgumentException(message);
        }
    }

    public static <T> void checkNotEmpty(T[] array, String template, Object... args) {
        if (Objects.isNull(array) || array.length==0) {
            throw new IllegalArgumentException(String.format(template, args));
        }
    }
}
