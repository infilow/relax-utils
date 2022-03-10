package com.infilos.retry;

import java.time.Duration;

public interface BackoffStrategy {
    
    default void checkConfig(RetryConfig<?> config) {
    }

    Duration nextDelayToWait(int failedAttempts, Duration delayDuration);
    
    /**
     * Evaluate next delay depens on last value or error which caused the retry.
     */
    default Duration nextDelayToWait(int failedAttempts, Duration delayDuration, Object lastValue, Exception lastError) {
        return nextDelayToWait(failedAttempts, delayDuration);
    }
}
