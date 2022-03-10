package com.infilos.retry;

public class RetryException extends RuntimeException {

    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryException(String message) {
        super(message);
    }
}
