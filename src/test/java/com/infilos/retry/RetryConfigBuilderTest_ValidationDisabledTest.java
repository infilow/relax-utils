package com.infilos.retry;

import com.infilos.retry.backoff.*;
import org.junit.Test;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryConfigBuilderTest_ValidationDisabledTest {

    private RetryConfigBuilder<?> retryConfigBuilder;

    @BeforeMethod
    public void setup() {
        retryConfigBuilder = new RetryConfigBuilder<>(false);
    }

    @Test
    public void testSettingRetryOnAnyException() {
        RetryConfig<?> config = retryConfigBuilder
            .retryOnAnyError()
            .build();

        assertThat(config.shouldRetryOnAnyError()).isTrue();
    }

    @Test
    public void testSettingRetryOnSpecificExceptions() {
        RetryConfig<?> config = retryConfigBuilder
            .retryOnErrors(Arrays.asList(IllegalArgumentException.class, UnsupportedOperationException.class))
            .build();

        assertThat(config.getRetryOnErrorIncluding())
            .containsOnly(IllegalArgumentException.class, UnsupportedOperationException.class);
    }

    @Test
    public void testSettingMaxTries() {
        RetryConfig<?> config = retryConfigBuilder
            .withMaxAttempts(99)
            .build();

        assertThat(config.getMaxAttempts()).isEqualTo(99);
    }

    @Test
    public void testSettingDurationBetweenTries_duration() {
        Duration duration = Duration.of(60, ChronoUnit.MINUTES);

        RetryConfig<?> config = retryConfigBuilder
            .withDelayDuration(duration)
            .build();

        assertThat(config.getDelayDuration()).isEqualTo(duration);
    }

    @Test
    public void testSettingDurationBetweenTries_seconds() {
        RetryConfig<?> config = retryConfigBuilder
            .withDelayDuration(5, ChronoUnit.SECONDS)
            .build();

        assertThat(config.getDelayDuration().toMillis()).isEqualTo(5000);
    }

    @Test
    public void testSettingDurationBetweenTries_millis() {
        RetryConfig<?> config = retryConfigBuilder
            .withDelayDuration(5000, ChronoUnit.MILLIS)
            .build();

        assertThat(config.getDelayDuration().toMillis()).isEqualTo(5000);
    }

    @Test
    public void testSettingBackoffStrategy_exponential() {
        RetryConfig<?> config = retryConfigBuilder
            .withExponentialBackoff()
            .build();

        assertThat(config.getBackoffStrategy()).isInstanceOf(ExponentialBackoffStrategy.class);
    }

    @Test
    public void testSettingBackoffStrategy_fixed() {
        RetryConfig<?> config = retryConfigBuilder
            .withFixedBackoff()
            .build();

        assertThat(config.getBackoffStrategy()).isInstanceOf(FixedBackoffStrategy.class);
    }

    @Test
    public void testSettingBackoffStrategy_fibonacci() {
        RetryConfig<?> config = retryConfigBuilder
            .withFibonacciBackoff()
            .build();

        assertThat(config.getBackoffStrategy()).isInstanceOf(FibonacciBackoffStrategy.class);
    }
}