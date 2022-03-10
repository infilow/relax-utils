package com.infilos.retry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * @param <T> is the return value type.
 */
public class RetryConfig<T> {
    // retry depends on throwed error
    private final boolean retryOnAnyError;
    private final Set<Class<? extends Exception>> retryOnErrorIncluding;
    private final Set<Class<? extends Exception>> retryOnErrorExcluding;
    private final Function<Exception, Boolean> retryOnErrorMatcher;
    private final boolean retryOnErrorOfCausedBy;

    // retry depends on returned value
    private final boolean retryOnValue;
    private final Collection<T> retryOnValueIncluding;
    private final Collection<T> retryOnValueExcluding;
    private final Function<T, Boolean> retryOnValueMatcher;

    // attempt/delay/backoff
    private final Integer maxAttempts;
    private final Duration delayDuration;
    private final BackoffStrategy backoffStrategy;

    // listener
    private final RetryListener<T> afterFailTryListener;
    private final RetryListener<T> beforeNextTryListener;
    private final RetryListener<T> onSuccessListener;
    private final RetryListener<T> onFailureListener;
    private final RetryListener<T> onCompletionListener;
    
    // async thread pool
    private final ExecutorService executorService;

    RetryConfig(boolean retryOnAnyError,
                Set<Class<? extends Exception>> retryOnErrorIncluding,
                Set<Class<? extends Exception>> retryOnErrorExcluding,
                Function<Exception, Boolean> retryOnErrorMatcher,
                boolean retryOnErrorOfCausedBy,
                boolean retryOnValue,
                Collection<T> retryOnValueIncluding,
                Collection<T> retryOnValueExcluding,
                Function<T, Boolean> retryOnValueMatcher,
                Integer maxAttempts,
                Duration delayDuration,
                BackoffStrategy backoffStrategy,
                RetryListener<T> afterFailTryListener,
                RetryListener<T> beforeNextTryListener,
                RetryListener<T> onSuccessListener,
                RetryListener<T> onFailureListener,
                RetryListener<T> onCompletionListener,
                ExecutorService executorService) {
        this.retryOnAnyError = retryOnAnyError;
        this.retryOnErrorIncluding = retryOnErrorIncluding;
        this.retryOnErrorExcluding = retryOnErrorExcluding;
        this.retryOnErrorMatcher = retryOnErrorMatcher;
        this.retryOnErrorOfCausedBy = retryOnErrorOfCausedBy;

        this.retryOnValue = retryOnValue;
        this.retryOnValueIncluding = retryOnValueIncluding;
        this.retryOnValueExcluding = retryOnValueExcluding;
        this.retryOnValueMatcher = retryOnValueMatcher;

        this.maxAttempts = maxAttempts;
        this.delayDuration = delayDuration;
        this.backoffStrategy = backoffStrategy;

        this.afterFailTryListener = afterFailTryListener;
        this.beforeNextTryListener = beforeNextTryListener;
        this.onSuccessListener = onSuccessListener;
        this.onFailureListener = onFailureListener;
        this.onCompletionListener = onCompletionListener;
        
        this.executorService = executorService;
    }

    public boolean shouldRetryOnAnyError() {
        return retryOnAnyError;
    }

    public Collection<T> getRetryOnValueIncluding() {
        return retryOnValueIncluding;
    }

    public Collection<T> getRetryOnValueExcluding() {
        return retryOnValueExcluding;
    }

    public Function<Exception, Boolean> getRetryOnErrorMatcher() {
        return retryOnErrorMatcher;
    }

    public boolean shouldRetryOnErrorOfCausedBy() {
        return retryOnErrorOfCausedBy;
    }

    public boolean shouldRetryOnValue() {
        return retryOnValue;
    }

    public Set<Class<? extends Exception>> getRetryOnErrorIncluding() {
        return retryOnErrorIncluding;
    }

    public Set<Class<? extends Exception>> getRetryOnErrorExcluding() {
        return retryOnErrorExcluding;
    }

    public Function<T, Boolean> getRetryOnValueMatcher() {
        return retryOnValueMatcher;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public Duration getDelayDuration() {
        return delayDuration;
    }

    public BackoffStrategy getBackoffStrategy() {
        return backoffStrategy;
    }

    public RetryListener<T> getAfterFailTryListener() {
        return afterFailTryListener;
    }

    public RetryListener<T> getBeforeNextTryListener() {
        return beforeNextTryListener;
    }

    public RetryListener<T> getOnSuccessListener() {
        return onSuccessListener;
    }

    public RetryListener<T> getOnFailureListener() {
        return onFailureListener;
    }

    public RetryListener<T> getOnCompletionListener() {
        return onCompletionListener;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public String toString() {
        return "RetryConfig{" +
            "retryOnAnyError=" + retryOnAnyError +
            ", retryOnErrorIncluding=" + retryOnErrorIncluding +
            ", retryOnErrorExcluding=" + retryOnErrorExcluding +
            ", retryOnErrorMatcher=" + retryOnErrorMatcher +
            ", retryOnErrorOfCausedBy=" + retryOnErrorOfCausedBy +
            ", retryOnValue=" + retryOnValue +
            ", retryOnValueIncluding=" + retryOnValueIncluding +
            ", retryOnValueExcluding=" + retryOnValueExcluding +
            ", retryOnValueMatcher=" + retryOnValueMatcher +
            ", maxAttempts=" + maxAttempts +
            ", delayDuration=" + delayDuration +
            ", backoffStrategy=" + backoffStrategy +
            ", afterFailTryListener=" + afterFailTryListener +
            ", beforeNextTryListener=" + beforeNextTryListener +
            ", onSuccessListener=" + onSuccessListener +
            ", onFailureListener=" + onFailureListener +
            ", onCompletionListener=" + onCompletionListener +
            '}';
    }
    
    /**
     * Rebuid nice for test.
     */
    @SuppressWarnings("unchecked")
    public RetryConfigBuilder<T> builder() {
        RetryConfigBuilder<T> builder = new RetryConfigBuilder<>();
        if(retryOnAnyError) {
            builder.retryOnAnyError();
        }
        if(!retryOnErrorIncluding.isEmpty()) {
            builder.retryOnErrors(new ArrayList<>(retryOnErrorIncluding));    
        }
        if(!retryOnErrorExcluding.isEmpty()) {
            builder.retryOnAnyErrorExclude(new ArrayList<>(retryOnErrorExcluding));    
        }
        if(Objects.nonNull(retryOnErrorMatcher)) {
            builder.retryOnErrorMatch(retryOnErrorMatcher);    
        }
        if(retryOnErrorOfCausedBy) {
            builder.retryOnErrorOfCausedBy();
        }
        if(!retryOnValueIncluding.isEmpty()) {
            builder.retryOnValues((T[]) retryOnValueIncluding.toArray());    
        }
        if(!retryOnValueExcluding.isEmpty()) {
            builder.retryOnAnyValueExclude((T[]) retryOnValueExcluding.toArray());    
        }
        if(Objects.nonNull(retryOnValueMatcher)) {
            builder.retryOnValueMatch(retryOnValueMatcher);    
        }
        builder.withMaxAttempts(maxAttempts);
        builder.withDelayDuration(delayDuration);
        builder.withCustomBackoff(backoffStrategy);
        builder.listenAfterFailTry(afterFailTryListener);
        builder.listenBeforeNextTry(beforeNextTryListener);
        builder.listenSuccedTry(onSuccessListener);
        builder.listenFailedTry(onFailureListener);
        builder.listenCompleteTry(onCompletionListener);
        builder.asyncThreadPool(executorService);
        
        return builder;
    }
}
