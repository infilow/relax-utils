package com.infilos.utils.retry;

import com.infilos.utils.Require;

/**
 * Retry plan's exactly matched result, with matched strategy and remain rules.
 */
public class RetryExecution<T> {
    private final T strategy;
    private final RetryPlan<T> retryPlan;

    RetryExecution(T stragety, RetryPlan<T> retryPlan) {
        Require.checkNotNull(stragety);
        Require.checkNotNull(retryPlan);
        
        this.strategy = stragety;
        this.retryPlan = retryPlan;
    }

    /** Returns the strategy to handle the current exception. Up to caller to interpret. */
    public T strategy() {
        return strategy;
    }

    /** Returns the exception plan for remaining exceptions. */
    public RetryPlan<T> remainingRetryPlan() {
        return retryPlan;
    }
}
