package com.infilos.retry.execute;

import com.infilos.retry.*;
import com.infilos.retry.RetryConfig;

import java.util.concurrent.*;

/**
 * Implementation that kicks off each retry request in its own separate thread that does not block the thread the execution is called from. If you provide an ExecutorService, it will be used when creating threads.
 *
 * @param <T> The type that is returned by the Callable (eg: Boolean, Void, Object, etc)
 */
public class RetryAsyncExecutor<T> implements RetryExecutor<T, CompletableFuture<RetryStatus<T>>> {

    private final RetryConfig<T> config;

    public RetryAsyncExecutor(RetryConfig<T> config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<RetryStatus<T>> execute(Callable<T> callable) {
        return execute(callable, null);
    }

    @Override
    public CompletableFuture<RetryStatus<T>> execute(Callable<T> callable, String operation) {
        RetrySyncExecutor<T> synchronousExecutor = new RetrySyncExecutor<>(config);

        CompletableFuture<RetryStatus<T>> completableFuture = new CompletableFuture<>();

        if (config.getExecutorService() != null) {
            config.getExecutorService().submit(() -> executeFuture(callable, operation, synchronousExecutor, completableFuture));
        } else {
            (new Thread(() -> executeFuture(callable, operation, synchronousExecutor, completableFuture))).start();
        }

        return completableFuture;
    }

    private void executeFuture(Callable<T> callable, String callName, RetrySyncExecutor<T> synchronousExecutor, CompletableFuture<RetryStatus<T>> completableFuture) {
        try {
            RetryStatus<T> status = synchronousExecutor.execute(callable, callName);
            completableFuture.complete(status);
        } catch (Throwable t) {
            completableFuture.completeExceptionally(t);
        }
    }

    public RetryConfig<T> getConfig() {
        return config;
    }
}