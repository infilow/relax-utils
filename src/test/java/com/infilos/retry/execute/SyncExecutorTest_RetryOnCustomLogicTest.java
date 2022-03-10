package com.infilos.retry.execute;

import com.infilos.retry.*;
import com.infilos.retry.exception.RetryEscapedException;
import com.infilos.retry.exception.RetryTiredException;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;

public class SyncExecutorTest_RetryOnCustomLogicTest {

    @Test
    public void verifyShouldRetryOnExceptionMessage() {
        RetryConfig<?> config = Retry.config()
            .retryOnErrorMatch(ex -> ex.getMessage().contains("should retry!"))
            .withFixedBackoff()
            .withDelayDuration(Duration.ofMillis(1))
            .withMaxAttempts(3)
            .build();

        try {
            Retry.runSync(config, () -> {
                throw new RuntimeException("should retry!");
            });
            fail();
        } catch (RetryTiredException e) {
            assertThat(e.getStatus().getTotalTries()).isEqualTo(3);
        }
    }

    @Test(expectedExceptions = RetryEscapedException.class)
    public void verifyShouldNotRetryOnExceptionMessage() {
        RetryConfig<?> config = Retry.config()
            .retryOnErrorMatch(ex -> ex.getMessage().contains("should retry!"))
            .withFixedBackoff()
            .withDelayDuration(Duration.ofMillis(1))
            .withMaxAttempts(3)
            .build();

        Retry.runSync(config, () -> {
            throw new RuntimeException("should NOT retry!");
        });
    }

    @Test
    public void verifyShouldRetryOnCustomException() {
        RetryConfig<?> config = Retry.config()
            .retryOnErrorMatch(ex -> ((CustomTestException) ex).getSomeValue() > 0)
            .withFixedBackoff()
            .withDelayDuration(Duration.ofMillis(1))
            .withMaxAttempts(3)
            .build();

        try {
            Retry.runSync(config, () -> {
                throw new CustomTestException("should retry!", 100);
            });
            fail();
        } catch (RetryTiredException e) {
            assertThat(e.getStatus().getTotalTries()).isEqualTo(3);
        }
    }

    @Test(expectedExceptions = RetryEscapedException.class)
    public void verifyShouldNotRetryOnCustomException() {
        RetryConfig<?> config = Retry.config()
            .retryOnErrorMatch(ex -> ((CustomTestException) ex).getSomeValue() > 0)
            .withFixedBackoff()
            .withDelayDuration(Duration.ofMillis(1))
            .withMaxAttempts(3)
            .build();

        Retry.runSync(config, () -> {
            throw new CustomTestException("test message", -100);
        });
    }

}