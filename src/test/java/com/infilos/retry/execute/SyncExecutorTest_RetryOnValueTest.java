package com.infilos.retry.execute;

import com.infilos.retry.*;
import com.infilos.retry.exception.RetryTiredException;
import com.infilos.utils.Loggable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.fail;

public class SyncExecutorTest_RetryOnValueTest {

    private RetryConfigBuilder<String> retryConfigBuilder;

    @BeforeMethod
    public void setup() {
        this.retryConfigBuilder = new RetryConfigBuilder<>(false);
    }

    @Test
    public void verifyRetryOnStringValue_shouldRetry() {
        Callable<String> callable = () -> "should retry!";

        RetryConfig<String> config = retryConfigBuilder
            .retryOnAnyError()
            .retryOnValue("should retry!")
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryOccurs(config, callable);
    }

    @Test
    public void verifyRetryOnStringValue_shouldNotRetry() {
        Callable<String> callable = () -> "should NOT retry!";

        RetryConfig<String> config = retryConfigBuilder
            .retryOnAnyError()
            .retryOnValue("should retry!")
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryDoesNotOccur(config, callable);
    }

    @Test
    public void verifyRetryOnBooleanValue_shouldRetry() {
        Callable<Boolean> callable = () -> false;

        RetryConfig<Boolean> config = Retry.config(Boolean.class)
            .retryOnAnyError()
            .retryOnValue(false)
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryOccurs(config, callable);
    }

    @Test
    public void verifyRetryOnBooleanValue_shouldNotRetry() {
        Loggable.switchRootLevel(Loggable.Level.TRACE);
        
        Callable<Boolean> callable = () -> true;

        RetryConfig<Boolean> config = Retry.config(Boolean.class)
            .retryOnAnyError()
            .retryOnValue(false)
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryDoesNotOccur(config, callable);
    }

    @Test
    public void verifyRetryOnComplexValue_shouldRetry() {
        Callable<RetryOnValueTestObject> callable
            = () -> new RetryOnValueTestObject("should retry on this value!");

        RetryConfig<RetryOnValueTestObject> config = Retry.config(RetryOnValueTestObject.class)
            .retryOnAnyError()
            .retryOnValue(new RetryOnValueTestObject("should retry on this value!"))
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryOccurs(config, callable);
    }

    @Test
    public void verifyRetryOnComplexValue_shouldNotRetry() {
        Callable<RetryOnValueTestObject> callable
            = () -> new RetryOnValueTestObject("should NOT retry on this value!");

        RetryConfig<RetryOnValueTestObject> config = Retry.config(RetryOnValueTestObject.class)
            .retryOnAnyError()
            .retryOnValue(new RetryOnValueTestObject("should retry on this value!"))
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryDoesNotOccur(config, callable);
    }

    @Test
    public void verifyRetryOnValueMatcher_shouldNotRetry() {
        Callable<RetryOnValueTestObject> callable
            = () -> new RetryOnValueTestObject("should NOT retry on this value!");

        RetryConfig<RetryOnValueTestObject> config = Retry.config(RetryOnValueTestObject.class)
            .retryOnAnyError()
            .retryOnValueMatch(value -> value.equals(new RetryOnValueTestObject("should retry on this value!")))
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryDoesNotOccur(config, callable);
    }

    @Test
    public void verifyRetryOnValueMatcher_shouldRetry() {
        Callable<RetryOnValueTestObject> callable
            = () -> new RetryOnValueTestObject("should retry on this value!");

        RetryConfig<RetryOnValueTestObject> config = Retry.config(RetryOnValueTestObject.class)
            .retryOnAnyError()
            .retryOnValueMatch(value -> value.equals(new RetryOnValueTestObject("should retry on this value!")))
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryOccurs(config, callable);
    }

    @Test
    public void verifyRetryOnReturnValues_shouldRetry() {
        Callable<RetryOnValueTestObject> callable
            = () -> new RetryOnValueTestObject("500 ERROR");

        RetryConfig<RetryOnValueTestObject> config = Retry.config(RetryOnValueTestObject.class)
            .retryOnAnyError()
            .retryOnValues(
                new RetryOnValueTestObject("500 ERROR"),
                new RetryOnValueTestObject("501 NOT IMPLEMENTED")
            )
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryOccurs(config, callable, 3);
    }

    @Test
    public void verifyRetryOnReturnValuesExcluding_shouldRetry() {
        Callable<RetryOnValueTestObject> callable
            = () -> new RetryOnValueTestObject("500 ERROR");

        RetryConfig<RetryOnValueTestObject> config = Retry.config(RetryOnValueTestObject.class)
            .retryOnAnyError()
            .retryOnAnyValueExclude(new RetryOnValueTestObject("200 OK"))
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryOccurs(config, callable, 3);
    }

    @Test
    public void verifyRetryOnReturnValuesExcluding_shouldNotRetry() {
        Callable<RetryOnValueTestObject> callable
            = () -> new RetryOnValueTestObject("201 CREATED");

        RetryConfig<RetryOnValueTestObject> config = Retry.config(RetryOnValueTestObject.class)
            .retryOnAnyError()
            .retryOnAnyValueExclude(new RetryOnValueTestObject("200 OK"), new RetryOnValueTestObject("201 CREATED"))
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(3)
            .withFixedBackoff()
            .build();

        assertRetryDoesNotOccur(config, callable);
    }

    @Test
    public void verifyRetryOnValueAndExceptionInSameCall() {
        final Random random = new Random();
        Callable<Boolean> callable = () -> {
            if (random.nextBoolean()) {
                return false;
            } else {
                throw new FileNotFoundException();
            }
        };

        RetryConfig<Boolean> config = Retry.config(Boolean.class)
            .retryOnError(FileNotFoundException.class)
            .retryOnValue(false)
            .withDelayDuration(Duration.ZERO)
            .withMaxAttempts(100)
            .withFixedBackoff()
            .build();

        assertRetryOccurs(config, callable, 100);
    }

    private <T> void assertRetryOccurs(RetryConfig<T> config, Callable<T> callable, int expectedNumberOfTries) {
        try {
            Retry.runSync(config, callable);
            fail("Expected RetryTiredException but one wasn't thrown!");
        } catch (RetryTiredException e) {
            assertThat(e.getStatus().hasSucced()).isFalse();
            assertThat(e.getStatus().getTotalTries()).isEqualTo(expectedNumberOfTries);
        }
    }

    private <T> void assertRetryOccurs(RetryConfig<T> config, Callable<T> callable) {
        assertRetryOccurs(config, callable, 3);
    }

    private <T> void assertRetryDoesNotOccur(RetryConfig<T> config, Callable<T> callable) {
        RetryStatus<T> status = Retry.runSync(config, callable);
        assertThat(status.hasSucced()).isTrue();
        assertThat(status.getTotalTries()).isEqualTo(1);
    }

    private static class RetryOnValueTestObject {

        private final String blah;

        RetryOnValueTestObject(String blah) {
            this.blah = blah;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            RetryOnValueTestObject that = (RetryOnValueTestObject) o;

            return Objects.equals(blah, that.blah);
        }

        @Override
        public int hashCode() {
            return blah != null ? blah.hashCode() : 0;
        }
    }

}
