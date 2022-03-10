package com.infilos.retry.backoff;

import com.infilos.retry.BackoffStrategy;
import com.infilos.retry.RetryConfig;
import com.infilos.retry.exception.RetryInvalidConfigException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class FibonacciBackoffStrategy implements BackoffStrategy {

    public static final int MAX_NUM_OF_FIB_NUMBERS = 25;
    private final List<Integer> fibonacciNumbers;

    public FibonacciBackoffStrategy() {
        fibonacciNumbers = new ArrayList<>();

        fibonacciNumbers.add(0);
        fibonacciNumbers.add(1);

        for (int i = 0; i < MAX_NUM_OF_FIB_NUMBERS; i++) {
            int nextFibNum = fibonacciNumbers.get(i) + fibonacciNumbers.get(i + 1);
            fibonacciNumbers.add(nextFibNum);
        }
    }

    @Override
    public Duration nextDelayToWait(int failedAttempts, Duration delayDuration) {
        int fibNumber;
        try {
            fibNumber = fibonacciNumbers.get(failedAttempts);
        } catch (IndexOutOfBoundsException e) {
            fibNumber = fibonacciNumbers.get(MAX_NUM_OF_FIB_NUMBERS - 1);
        }
        return Duration.ofMillis(delayDuration.toMillis() * fibNumber);
    }

    @Override
    public void checkConfig(RetryConfig<?> config) {
        if (null == config.getDelayDuration()) {
            throw new RetryInvalidConfigException("Retry config must specify the delay between retries!");
        }
    }

    public List<Integer> getFibonacciNumbers() {
        return fibonacciNumbers;
    }
}
