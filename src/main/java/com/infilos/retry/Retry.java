package com.infilos.retry;

import com.infilos.retry.execute.RetryAsyncExecutor;
import com.infilos.retry.execute.RetrySyncExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class Retry {

    /**
     * <pre>{@code
     * Retry.<T>config();
     * }</pre>
     */
    public static <T> RetryConfigBuilder<T> config() {
        return new RetryConfigBuilder<>();
    }

    /**
     * <pre>{@code
     * Retry.config(T.class);
     * }</pre>
     */
    public static <T> RetryConfigBuilder<T> config(Class<T> returnType) {
        return new RetryConfigBuilder<>();
    }
    
    public static <T> RetryStatus<T> runSync(RetryConfig<T> config, Callable<T> callable) {
        return new RetrySyncExecutor<>(config).execute(callable, null);
    }

    public static <T> RetryStatus<T> runSync(RetryConfig<T> config, String operation, Callable<T> callable) {
        return new RetrySyncExecutor<>(config).execute(callable, operation);
    }

    public static <T> CompletableFuture<RetryStatus<T>> runAsync(RetryConfig<T> config, Callable<T> callable) {
        return new RetryAsyncExecutor<>(config).execute(callable, null);
    }

    public static <T> CompletableFuture<RetryStatus<T>> runAsync(RetryConfig<T> config, String operation, Callable<T> callable) {
        return new RetryAsyncExecutor<>(config).execute(callable, operation);
    }
}
