package com.infilos.retry.backoff;

import com.infilos.retry.BackoffStrategy;

import java.time.Duration;

public class NoWaitBackoffStrategy implements BackoffStrategy {

    @Override
    public Duration nextDelayToWait(int failedAttempts, Duration delayDuration) {
        return Duration.ZERO;
    }
}
