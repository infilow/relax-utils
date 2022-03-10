package com.infilos.retry.execute;

import com.infilos.retry.*;
import com.infilos.retry.exception.RetryEscapedException;
import com.infilos.retry.exception.RetryTiredException;
import org.mockito.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncExecutorTest {

    @Mock
    private BackoffStrategy mockBackOffStrategy;

    private RetryConfigBuilder<Boolean> retryConfigBuilder;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.retryConfigBuilder = new RetryConfigBuilder<>(false);
    }

    @Test
    public void verifyReturningObjectFromCallSucceeds() throws Exception {
        Callable<Boolean> callable = () -> true;

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .withMaxAttempts(5)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();
        
        RetryStatus<Boolean> status = Retry.runSync(retryConfig, callable);
        assertThat(status.hasSucced()).isTrue();
    }

    @Test(expectedExceptions = {RetryTiredException.class})
    public void verifyExceptionFromCallThrowsCallFailureException() throws Exception {
        Callable<Boolean> callable = () -> {
            throw new RuntimeException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnAnyError()
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryTiredException.class})
    public void shouldMatchExceptionCauseAndRetry() throws Exception {
        Callable<Boolean> callable = () -> {
            throw new Exception(new CustomTestException("message", 3));
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnErrorOfCausedBy()
            .retryOnError(CustomTestException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryTiredException.class})
    public void shouldMatchExceptionCauseAtGreaterThanALevelDeepAndRetry() throws Exception {
        class CustomException extends Exception {

            CustomException(Throwable cause) {
                super(cause);
            }
        }
        Callable<Boolean> callable = () -> {
            throw new Exception(new CustomException(new RuntimeException(new IOException())));
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnErrorOfCausedBy()
            .retryOnError(IOException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryEscapedException.class})
    public void shouldThrowUnexpectedIfThrownExceptionCauseDoesNotMatchRetryExceptions() throws Exception {
        Callable<Boolean> callable = () -> {
            throw new Exception(new CustomTestException("message", 3));
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnErrorOfCausedBy()
            .retryOnError(IOException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryEscapedException.class})
    public void verifySpecificSuperclassExceptionThrowsUnexpectedException() throws Exception {
        Callable<Boolean> callable = () -> {
            throw new Exception();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnError(IOException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryTiredException.class})
    public void verifySpecificSubclassExceptionRetries() throws Exception {
        Callable<Boolean> callable = () -> {
            throw new IOException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnError(Exception.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryTiredException.class})
    public void verifyExactSameSpecificExceptionThrowsCallFailureException() throws Exception {
        Callable<Boolean> callable = () -> {
            throw new IllegalArgumentException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnError(IllegalArgumentException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryEscapedException.class})
    public void verifyUnspecifiedExceptionCausesUnexpectedCallFailureException() throws Exception {
        Callable<Boolean> callable = () -> {
            throw new IllegalArgumentException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnError(UnsupportedOperationException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test
    public void verifyStatusIsPopulatedOnSuccessfulCall() throws Exception {
        Callable<Boolean> callable = () -> true;

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .withMaxAttempts(5)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        RetryStatus<Boolean> status = Retry.runSync(retryConfig, callable);

        assertThat(status.getResult()).isNotNull();
        assertThat(status.hasSucced()).isTrue();
        assertThat(status.getOperation()).isNullOrEmpty();
        assertThat(status.getTotalDuration().toMillis()).isCloseTo(0, within(25L));
        assertThat(status.getTotalTries()).isEqualTo(1);
    }

    @Test
    public void verifyStatusIsPopulatedOnFailedCall() throws Exception {
        Callable<Boolean> callable = () -> {
            throw new FileNotFoundException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .withMaxAttempts(5)
            .retryOnAnyError()
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        try {
            Retry.runSync(retryConfig, "TestCall", callable);
            fail("RetryTiredException wasn't thrown!");
        } catch (RetryTiredException e) {
            RetryStatus<Boolean> status = e.getStatus();
            assertThat(status.getResult()).isNull();
            assertThat(status.hasSucced()).isFalse();
            assertThat(status.getOperation()).isEqualTo("TestCall");
            assertThat(status.getTotalDuration().toMillis()).isCloseTo(0, within(25L));
            assertThat(status.getTotalTries()).isEqualTo(5);
            assertThat(e.getCause()).isExactlyInstanceOf(FileNotFoundException.class);
        }
    }

    @Test
    public void verifyReturningObjectFromCallable() throws Exception {
        Callable<String> callable = () -> "test";

        RetryConfig<String> retryConfig = Retry.<String>config()
            .disableValidation()
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .build();

        RetryStatus<String> status = Retry.runSync(retryConfig, callable);

        assertThat(status.getResult()).isEqualTo("test");
    }

    @Test
    public void verifyNullCallResultCountsAsValidResult() throws Exception {
        Callable<String> callable = () -> null;

        RetryConfig<String > retryConfig = Retry.config(String.class)
            .disableValidation()
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .build();

        try {
            Retry.runSync(retryConfig, callable);
        } catch (RetryTiredException e) {
            RetryStatus<String> status = e.getStatus();
            assertThat(status.getResult()).isNull();
            assertThat(status.hasSucced()).isTrue();
        }
    }

    @Test
    public void verifyRetryingIndefinitely() throws Exception {
        Callable<Boolean> callable = () -> {
            Random random = new Random();
            if (random.nextInt(10000) == 0) {
                return true;
            }
            throw new IllegalArgumentException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .withInfiniteAttempts()
            .retryOnAnyError()
            .withFixedBackoff()
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .build();

        try {
            Retry.runSync(retryConfig, callable);
        } catch (RetryTiredException e) {
            fail("Retries should never be exhausted!");
        }
    }

    @Test
    public void verifyRetryPolicyTimeoutIsUsed() {
        Exception error = new RuntimeException();
        Callable<Object> callable = () -> {
            throw error;
        };

        Duration delayBetweenTriesDuration = Duration.ofSeconds(17);
        when(mockBackOffStrategy.nextDelayToWait(1, delayBetweenTriesDuration,null, error)).thenReturn(Duration.ofSeconds(5));

        RetryConfig<Object> retryConfig = Retry.config()
            .withMaxAttempts(2)
            .retryOnAnyError()
            .withDelayDuration(delayBetweenTriesDuration)
            .withCustomBackoff(mockBackOffStrategy)
            .build();

        final long before = System.currentTimeMillis();
        try {
            Retry.runSync(retryConfig, callable);
        } catch (RetryTiredException ignored) {
        }

        assertThat(System.currentTimeMillis() - before).isGreaterThan(5000);
        verify(mockBackOffStrategy).nextDelayToWait(1, delayBetweenTriesDuration, null, error);
    }

    @Test
    public void verifyNoDurationSpecifiedSucceeds() {
        Callable<String> callable = () -> "test";

        RetryConfig<String> noWaitConfig =Retry.config(String.class)
            .withMaxAttempts(1)
            .withNoDelayBackoff()
            .build();

        RetryStatus<String> status = Retry.runSync(noWaitConfig, callable);

        assertThat(status.getResult()).isEqualTo("test");
    }
}

