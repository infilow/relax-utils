package com.infilos.retry.exception;

import com.infilos.retry.RetryException;

public class RetryInvalidConfigException extends RetryException {
    public RetryInvalidConfigException(String message) {
        super(message);
    }
}
