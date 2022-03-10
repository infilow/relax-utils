package com.infilos.retry;

import com.infilos.retry.exception.RetryInvalidConfigException;
import org.junit.Test;
import org.testng.TestException;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.fail;

public class RetryConfigBuilderTest_WithValidationTest {
    private RetryConfigBuilder<?> retryConfigBuilder;

    @BeforeMethod
    public void setup() {
        retryConfigBuilder = new RetryConfigBuilder<>(true);
    }

    @Test
    public void verifyNoBackoffStrategyThrowsException() {
        try {
            retryConfigBuilder
                .withMaxAttempts(1)
                .withDelayDuration(1, ChronoUnit.SECONDS)
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.MUST_SPECIFY_BACKOFF);
        }
    }

    @Test
    public void verifyTwoBackoffStrategiesThrowsException() {
        try {
            retryConfigBuilder
                .withMaxAttempts(1)
                .withDelayDuration(1, ChronoUnit.SECONDS)
                .withExponentialBackoff()
                .withFibonacciBackoff()
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.CAN_ONLY_SPECIFY_ONE_BACKOFF);
        }
    }

    @Test
    public void verifyNoDelayThrowsException_exponentialBackoff() {
        try {
            retryConfigBuilder
                .withMaxAttempts(1)
                .withExponentialBackoff()
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo("Retry config must specify the delay between retries!");
        }
    }

    @Test
    public void verifyNoDelayThrowsException_fixedBackoff() {
        try {
            retryConfigBuilder
                .withMaxAttempts(1)
                .withFixedBackoff()
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo("Retry config must specify the delay between retries!");
        }
    }

    @Test
    public void verifyNoDelayThrowsException_randomBackoff() {
        try {
            retryConfigBuilder
                .withMaxAttempts(1)
                .withRandomBackoff()
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo("Retry config must specify the delay between retries!");
        }
    }

    @Test
    public void verifyNoDelayThrowsException_fibonacciBackoff() {
        try {
            retryConfigBuilder
                .withMaxAttempts(1)
                .withRandomBackoff()
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo("Retry config must specify the delay between retries!");
        }
    }

    @Test
    public void verifyNoDelayDoesNotThrowException_noWaitBackoff() {
        retryConfigBuilder
            .withMaxAttempts(1)
            .withNoDelayBackoff()
            .build();
    }

    @Test
    public void verifyNoMaxTriesThrowsException() {
        try {
            retryConfigBuilder
                .withDelayDuration(1, ChronoUnit.SECONDS)
                .withExponentialBackoff()
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.MUST_SPECIFY_MAX_ATTEMPT);
        }
    }

    @Test
    public void verifyZeroMaxTriesThrowsException() {
        try {
            retryConfigBuilder
                .withDelayDuration(1, ChronoUnit.SECONDS)
                .withExponentialBackoff()
                .withMaxAttempts(0)
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.MUST_SPECIFY_MAX_TRIES_ABOVE_0);
        }
    }

    @Test
    public void verifyMaxRetriesSpecifiedTwiceThrowsException_numberAndIndefinite() {
        try {
            retryConfigBuilder
                .withMaxAttempts(5)
                .withInfiniteAttempts()
                .withNoDelayBackoff()
                .retryOnAnyError()
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.ALREADY_SPECIFIED_ATTEMPT);
        }
    }

    @Test
    public void verifyMaxRetriesSpecifiedTwiceThrowsException_twoNumbers() {
        try {
            retryConfigBuilder
                .withMaxAttempts(5)
                .withMaxAttempts(50)
                .withNoDelayBackoff()
                .retryOnAnyError()
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.ALREADY_SPECIFIED_ATTEMPT);
        }
    }

    @Test
    public void verifySpecifyingMultipleBuildInAndCustomExceptionStrategiesThrowsException_anyException() {
        try {
            retryConfigBuilder
                .retryOnAnyError()
                .retryOnErrorMatch(ex -> ex.getMessage().contains("should retry!"))
                .withFixedBackoff()
                .withDelayDuration(Duration.ofMillis(1))
                .withMaxAttempts(3)
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.CAN_ONLY_SPECIFY_CUSTOM_EXCEPTION_STRAT);
        }
    }

    @Test
    public void verifySpecifyingMultipleBuildInAndCustomExceptionStrategiesThrowsException_specificExceptions() {
        try {
            retryConfigBuilder
                .retryOnError(TestException.class)
                .retryOnErrorMatch(ex -> ex.getMessage().contains("should retry!"))
                .withFixedBackoff()
                .withDelayDuration(Duration.ofMillis(1))
                .withMaxAttempts(3)
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.CAN_ONLY_SPECIFY_CUSTOM_EXCEPTION_STRAT);
        }
    }

    @Test
    public void verifySpecifyingMultipleBuildInAndCustomExceptionStrategiesThrowsException_excludingExceptions() {
        try {
            retryConfigBuilder
                .retryOnAnyErrorExclude(TestException.class)
                .retryOnErrorMatch(ex -> ex.getMessage().contains("should retry!"))
                .withFixedBackoff()
                .withDelayDuration(Duration.ofMillis(1))
                .withMaxAttempts(3)
                .build();
            fail("Expected RetryInvalidConfigException but one wasn't thrown!");
        } catch (RetryInvalidConfigException e) {
            assertThat(e.getMessage())
                .isEqualTo(RetryConfigBuilder.CAN_ONLY_SPECIFY_CUSTOM_EXCEPTION_STRAT);
        }
    }

    @Test(expected = RetryInvalidConfigException.class)
    public void shouldNotAllowNegativeDelayBetweenRetries() {
        retryConfigBuilder
            .withFixedBackoff()
            .withInfiniteAttempts()
            .withDelayDuration(Duration.of(-1, ChronoUnit.SECONDS))
            .build();
    }

    @Test(expected = RetryInvalidConfigException.class)
    public void shouldNotAllowNegativeMaxNumberOfTries() {
        retryConfigBuilder
            .withFixedBackoff()
            .withMaxAttempts(-1)
            .build();
    }
}