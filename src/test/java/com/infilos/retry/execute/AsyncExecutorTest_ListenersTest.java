package com.infilos.retry.execute;

import com.infilos.retry.Retry;
import com.infilos.retry.RetryConfig;
import org.testng.annotations.Test;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncExecutorTest_ListenersTest {

    @Test
    public void verifyOnListener_resultHasTypeOfCallExecutor() throws Exception {
        List<String> methodCalls = new ArrayList<>();

        RetryConfig<String> config = Retry.config(String.class)
            .retryOnAnyError()
            .withMaxAttempts(5)
            .withDelayDuration(0, ChronoUnit.SECONDS)
            .withFixedBackoff()
            .listenSuccedTry(status -> {
                methodCalls.add("onSuccess");
                assertThat(status.getResult()).isInstanceOf(String.class);
            })
            .listenCompleteTry(status -> {
                methodCalls.add("onCompletion");
                assertThat(status.getResult()).isInstanceOf(String.class);
            })
            .asyncThreadPool(Executors.newFixedThreadPool(5))
            .build();

        Retry.runAsync(config, () -> "").get();
        assertThat(methodCalls).contains("onSuccess", "onCompletion");
    }
}

