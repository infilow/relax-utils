package com.infilos.retry;

import com.infilos.retry.exception.RetryEscapedException;
import com.infilos.retry.exception.RetryTiredException;

import java.util.concurrent.Callable;

public interface RetryExecutor<T, S> {

    S execute(Callable<T> callable) throws RetryTiredException, RetryEscapedException;

    S execute(Callable<T> callable, String operation) throws RetryTiredException, RetryEscapedException;
}
