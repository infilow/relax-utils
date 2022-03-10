package com.infilos.retry.execute;

import com.infilos.retry.*;
import com.infilos.retry.exception.RetryEscapedException;
import com.infilos.retry.exception.RetryTiredException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class SyncExecutorTest_RetryOnAnyExcludingTest {

    private RetryConfigBuilder<Boolean> retryConfigBuilder;

    @BeforeMethod
    public void setup() {
        this.retryConfigBuilder = new RetryConfigBuilder<>(false);
    }

    @Test(expectedExceptions = {RetryEscapedException.class})
    public void verifyRetryOnAnyExcludingThrowsCallFailureException() {
        Callable<Boolean> callable = () -> {
            throw new UnsupportedOperationException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnAnyErrorExclude(UnsupportedOperationException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryTiredException.class})
    public void verifyRetryOnAnyExcludingCallSucceeds() {
        Callable<Boolean> callable = () -> {
            throw new IllegalArgumentException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnAnyErrorExclude(UnsupportedOperationException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = RetryEscapedException.class)
    public void verifySubclassOfExcludedExceptionThrowsUnexpectedException() {
        Callable<Boolean> callable = () -> {
            throw new FileNotFoundException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnAnyErrorExclude(IOException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = RetryTiredException.class)
    public void verifySuperclassOfExcludedExceptionDoesntThrowUnexpectedException() {
        Callable<Boolean> callable = () -> {
            throw new IOException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnAnyErrorExclude(FileNotFoundException.class)
            .withMaxAttempts(1)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryEscapedException.class})
    public void verifyMultipleExceptions_throwFirstExcludedException() {
        Callable<Boolean> callable = () -> {
            throw new IllegalArgumentException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnAnyErrorExclude(Arrays.asList(IllegalArgumentException.class, UnsupportedOperationException.class))
            .withMaxAttempts(5)
            .withNoDelayBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryEscapedException.class})
    public void verifyMultipleExceptions_throwSecondExcludedException() {
        Callable<Boolean> callable = () -> {
            throw new UnsupportedOperationException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnAnyErrorExclude(IllegalArgumentException.class)
            .retryOnAnyErrorExclude(UnsupportedOperationException.class)
            .withMaxAttempts(5)
            .withNoDelayBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

    @Test(expectedExceptions = {RetryTiredException.class})
    public void verifyMultipleExceptions_retryOnNotExcludedException() {
        Callable<Boolean> callable = () -> {
            throw new NullPointerException();
        };

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .retryOnAnyErrorExclude(Arrays.asList(IllegalArgumentException.class, UnsupportedOperationException.class))
            .withMaxAttempts(5)
            .withNoDelayBackoff()
            .build();

        Retry.runSync(retryConfig, callable);
    }

}