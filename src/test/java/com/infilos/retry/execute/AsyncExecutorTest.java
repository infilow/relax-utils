package com.infilos.retry.execute;

import com.infilos.retry.*;
import com.infilos.retry.exception.RetryEscapedException;
import com.infilos.retry.exception.RetryTiredException;
import com.infilos.utils.Loggable;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AsyncExecutorTest {

    private RetryConfig<Boolean> onAnyErrorConfig;

    private RetryConfig<Boolean> failOnAnyExceptionConfig;

    private ExecutorService executorService;

    @BeforeClass
    public void setup() {
        executorService = Executors.newFixedThreadPool(5);
        
        onAnyErrorConfig = Retry.config(Boolean.class)
            .retryOnAnyError()
            .withFixedBackoff()
            .withMaxAttempts(1)
            .withDelayDuration(Duration.ofMillis(0))
            .asyncThreadPool(executorService)
            .build();

        failOnAnyExceptionConfig = Retry.config(Boolean.class)
            .failOnAnyError()
            .withFixedBackoff()
            .withMaxAttempts(1)
            .withDelayDuration(Duration.ofMillis(0))
            .build();
    }

    @AfterClass
    public void teardown() {
        executorService.shutdown();
    }

    @Test
    public void verifyMultipleCalls_noExecutorService() throws Exception {
        Callable<Boolean> callable = () -> true;

        CompletableFuture<RetryStatus<Boolean>> future1 = Retry.runAsync(onAnyErrorConfig,callable);
        CompletableFuture<RetryStatus<Boolean>> future2 = Retry.runAsync(onAnyErrorConfig,callable);
        CompletableFuture<RetryStatus<Boolean>> future3 = Retry.runAsync(onAnyErrorConfig,callable);

        CompletableFuture<Void> combinedFuture
            = CompletableFuture.allOf(future1, future2, future3);
        combinedFuture.get();

        assertThat(future1).isDone();
        assertThat(future2).isDone();
        assertThat(future3).isDone();
    }

    @Test
    public void verifyOneCall_success_noExecutorService() throws Exception {
        Callable<Boolean> callable = () -> true;

        CompletableFuture<RetryStatus<Boolean>> future = Retry.runAsync(onAnyErrorConfig,callable);

        RetryStatus<Boolean> status = future.get();
        assertThat(future).isDone();
        assertThat(status.hasSucced()).isTrue();
    }

    @Test
    public void verifyOneCall_failDueToTooManyRetries_noExecutorService() {
        Callable<Boolean> callable = () -> {
            throw new RuntimeException();
        };

        CompletableFuture<RetryStatus<Boolean>> future = Retry.runAsync(onAnyErrorConfig, callable);

        assertThatThrownBy(future::get)
            .isExactlyInstanceOf(ExecutionException.class)
            .hasCauseExactlyInstanceOf(RetryTiredException.class);
    }

    @Test
    public void verifyOneCall_failDueToUnexpectedException_noExecutorService() {
        Callable<Boolean> callable = () -> {
            throw new RuntimeException();
        };

        CompletableFuture<RetryStatus<Boolean>> future = Retry.runAsync(failOnAnyExceptionConfig, callable);

        assertThatThrownBy(future::get)
            .isExactlyInstanceOf(ExecutionException.class)
            .hasCauseExactlyInstanceOf(RetryEscapedException.class);
    }

    @Test
    public void verifyMultipleCalls_withExecutorService() throws Exception {
        Callable<Boolean> callable = () -> true;

        CompletableFuture<RetryStatus<Boolean>> future1 = Retry.runAsync(onAnyErrorConfig, callable);
        CompletableFuture<RetryStatus<Boolean>> future2 = Retry.runAsync(onAnyErrorConfig, callable);
        CompletableFuture<RetryStatus<Boolean>> future3 = Retry.runAsync(onAnyErrorConfig, callable);

        CompletableFuture<Void> combinedFuture
            = CompletableFuture.allOf(future1, future2, future3);
        combinedFuture.get();

        assertThat(future1).isDone();
        assertThat(future2).isDone();
        assertThat(future3).isDone();
    }

    @Test
    public void verifyOneCall_success_withExecutorService() throws Exception {
        Callable<Boolean> callable = () -> true;
        
        CompletableFuture<RetryStatus<Boolean>> future = Retry.runAsync(onAnyErrorConfig, callable);

        RetryStatus<Boolean> status = future.get();
        assertThat(future).isDone();
        assertThat(status.hasSucced()).isTrue();
    }

    @Test
    public void verifyOneCall_failDueToTooManyRetries_withExecutorService() {
        Callable<Boolean> callable = () -> {
            throw new RuntimeException();
        };

        CompletableFuture<RetryStatus<Boolean>> future = Retry.runAsync(onAnyErrorConfig,callable);

        assertThatThrownBy(future::get)
            .isExactlyInstanceOf(ExecutionException.class)
            .hasCauseExactlyInstanceOf(RetryTiredException.class);
    }

    @Test
    public void verifyOneCall_failDueToUnexpectedException_withExecutorService() {
        Callable<Boolean> callable = () -> {
            throw new RuntimeException();
        };

        CompletableFuture<RetryStatus<Boolean>> future = Retry.runAsync(failOnAnyExceptionConfig,callable);

        assertThatThrownBy(future::get)
            .isExactlyInstanceOf(ExecutionException.class)
            .hasCauseExactlyInstanceOf(RetryEscapedException.class);
    }
}