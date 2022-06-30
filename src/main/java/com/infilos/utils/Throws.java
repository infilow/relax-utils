package com.infilos.utils;

import java.util.Objects;

public final class Throws {
    private Throws() {
    }

    public static String getMessage(Throwable e) {
        return Objects.isNull(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage();
    }

    public static String getClassMessage(Throwable e) {
        return String.format("%s(%s)", e.getClass().getSimpleName(), Objects.isNull(e.getMessage()) ? "" : e.getMessage());
    }

    public static <E extends Throwable> E propagateIfUnchecked(E exception) {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else if (exception instanceof Error) {
            throw (Error) exception;
        } else {
            return exception;
        }
    }

    public static RuntimeException runtime(String message, Throwable cause) {
        return new RuntimeException(message, cause);
    }

    public static RuntimeException runtime(String message) {
        return new RuntimeException(message, null);
    }

    public static RuntimeException runtime(Throwable cause) {
        return new RuntimeException("", cause);
    }

    public static RuntimeException runtime(String template, Object... args) {
        return new RuntimeException(String.format(template, args));
    }

    public static RuntimeException runtime(Throwable cause, String template, Object... args) {
        return new RuntimeException(String.format(template, args), cause);
    }
}
