package com.infilos.retry.backoff;

import com.infilos.retry.BackoffStrategy;
import com.infilos.retry.RetryConfig;
import com.infilos.retry.exception.RetryInvalidConfigException;

import java.time.Duration;

public class RandomExponentialBackoffStrategy implements BackoffStrategy {

    private final RandomBackoffStrategy randomBackoffStrategy;
    private final ExponentialBackoffStrategy exponentialBackoffStrategy;

    public RandomExponentialBackoffStrategy() {
        this.randomBackoffStrategy = new RandomBackoffStrategy(10);
        this.exponentialBackoffStrategy = new ExponentialBackoffStrategy();
    }

    @Override
    public Duration nextDelayToWait(int failedAttempts, Duration delayDuration) {
        Duration durationWaitFromExpBackoff
            = exponentialBackoffStrategy.nextDelayToWait(failedAttempts, delayDuration);

        return randomBackoffStrategy.nextDelayToWait(failedAttempts, durationWaitFromExpBackoff);
    }

    @Override
    public void checkConfig(RetryConfig<?> config) {
        if (null == config.getDelayDuration()) {
            throw new RetryInvalidConfigException("Retry config must specify the delay between retries!");
        }
    }
}

