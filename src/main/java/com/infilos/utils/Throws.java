package com.infilos.utils;

public final class Throws {
    private Throws() {
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
}
