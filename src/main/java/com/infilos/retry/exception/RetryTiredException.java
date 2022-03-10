package com.infilos.retry.exception;

import com.infilos.retry.RetryException;
import com.infilos.retry.RetryStatus;

/**
 * This exception represents a call execution that never succeeded after exhausting all retries.
 */
public class RetryTiredException extends RetryException {
    private final RetryStatus<?> status;

    public RetryTiredException(String message, Throwable cause, RetryStatus<?> status) {
        super(message, cause);
        this.status = status;
    }

    @SuppressWarnings("unchecked")
    public <T> RetryStatus<T> getStatus() {
        return (RetryStatus<T>) status;
    }
}
