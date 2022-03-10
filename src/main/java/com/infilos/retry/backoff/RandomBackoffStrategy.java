package com.infilos.retry.backoff;

import com.infilos.retry.BackoffStrategy;
import com.infilos.retry.RetryConfig;
import com.infilos.retry.exception.RetryInvalidConfigException;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class RandomBackoffStrategy implements BackoffStrategy {

    private final int maxMultiplier;

    public RandomBackoffStrategy() {
        this.maxMultiplier = 10;
    }

    public RandomBackoffStrategy(int maxMultiplier) {
        this.maxMultiplier = maxMultiplier;
    }

    @Override
    public Duration nextDelayToWait(int failedAttempts, Duration delayDuration) {
        int i = ThreadLocalRandom.current().nextInt(0, maxMultiplier - 1);
        return Duration.ofMillis(i * delayDuration.toMillis());
    }

    @Override
    public void checkConfig(RetryConfig<?> config) {
        if (null == config.getDelayDuration()) {
            throw new RetryInvalidConfigException("Retry config must specify the delay between retries!");
        }
    }
}
