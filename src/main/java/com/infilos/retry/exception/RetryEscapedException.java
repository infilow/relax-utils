package com.infilos.retry.exception;

import com.infilos.retry.RetryException;

/**
 * This exception represents when a call throws an exception that was not specified as one to retry on in the RetryConfig.
 */
public class RetryEscapedException extends RetryException {
    public RetryEscapedException(String message, Throwable cause) {
        super(message, cause);
    }
}
