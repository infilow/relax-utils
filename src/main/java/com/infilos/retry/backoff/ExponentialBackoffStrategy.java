package com.infilos.retry.backoff;

import com.infilos.retry.BackoffStrategy;
import com.infilos.retry.RetryConfig;
import com.infilos.retry.exception.RetryInvalidConfigException;

import java.time.Duration;

/**
 * AKA binary exponential backoff
 */
public class ExponentialBackoffStrategy implements BackoffStrategy {

    @Override
    public Duration nextDelayToWait(int failedAttempts, Duration delayDuration) {
        double exponentialMultiplier = Math.pow(2.0, failedAttempts - 1);
        double result = exponentialMultiplier * delayDuration.toMillis();
        long millisToWait = (long) Math.min(result, Long.MAX_VALUE);
        return Duration.ofMillis(millisToWait);
    }

    @Override
    public void checkConfig(RetryConfig<?> config) {
        if (null == config.getDelayDuration()) {
            throw new RetryInvalidConfigException("Retry config must specify the delay between retries!");
        }
    }
}