package com.infilos.retry;

import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryConfigBuilderTest_Listeners {

    private final RetryListener<Object> afterFailedTryListener = status -> {
    };
    private final RetryListener<Object> beforeNextTryListener = status -> {
    };
    private final RetryListener<Object> onSuccessListener = status -> {
    };
    private final RetryListener<Object> onFailureListener = status -> {
    };
    private final RetryListener<Object> onCompletionListener = status -> {
    };
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Test
    public void shouldBuildCallExecutorWithConfigAndListeners() {
        RetryConfig<?> config = Retry.config()
            .disableValidation()
            .listenAfterFailTry(afterFailedTryListener)
            .listenBeforeNextTry(beforeNextTryListener)
            .listenSuccedTry(onSuccessListener)
            .listenFailedTry(onFailureListener)
            .listenCompleteTry(onCompletionListener)
            .asyncThreadPool(executorService)
            .build();

        assertThat(config.getAfterFailTryListener()).isEqualTo(afterFailedTryListener);
        assertThat(config.getBeforeNextTryListener()).isEqualTo(beforeNextTryListener);
        assertThat(config.getOnSuccessListener()).isEqualTo(onSuccessListener);
        assertThat(config.getOnFailureListener()).isEqualTo(onFailureListener);
        assertThat(config.getOnCompletionListener()).isEqualTo(onCompletionListener);
        assertThat(config.getExecutorService()).isEqualTo(executorService);
    }
}