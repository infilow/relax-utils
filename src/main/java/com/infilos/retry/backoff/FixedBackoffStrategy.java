package com.infilos.retry.backoff;

import com.infilos.retry.BackoffStrategy;
import com.infilos.retry.RetryConfig;
import com.infilos.retry.exception.RetryInvalidConfigException;

import java.time.Duration;

public class FixedBackoffStrategy implements BackoffStrategy {

    @Override
    public Duration nextDelayToWait(int failedAttempts, Duration delayDuration) {
        return delayDuration;
    }

    @Override
    public void checkConfig(RetryConfig<?> config) {
        if (null == config.getDelayDuration()) {
            throw new RetryInvalidConfigException("Retry config must specify the delay between retries!");
        }
    }
}

