package com.infilos.retry;

import org.junit.Test;
import org.testng.annotations.BeforeMethod;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryConfigBuilderTest_SimpleDefaultsTest {
    
    private RetryConfigBuilder<Boolean> retryConfigBuilder;

    @BeforeMethod
    public void setup() {
        retryConfigBuilder = new RetryConfigBuilder<>(true);
    }

    @Test
    public void verifySimpleExponentialProfile() {
        Callable<Boolean> callable = () -> true;

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .exponentialBackoff5Tries5Sec()
            .build();

        RetryStatus<Boolean> results = Retry.runSync(retryConfig, callable);
        assertThat(results.hasSucced()).isTrue();
    }

    @Test
    public void verifySimpleFibonacciProfile() {
        Callable<Boolean> callable = () -> true;

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .fiboBackoff7Tries5Sec()
            .build();

        RetryStatus<Boolean> results = Retry.runSync(retryConfig, callable);
        assertThat(results.hasSucced()).isTrue();
    }

    @Test
    public void verifySimpleRandomExponentialProfile() {
        Callable<Boolean> callable = () -> true;

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .randomExpBackoff10Tries60Sec()
            .build();

        RetryStatus<Boolean> results = Retry.runSync(retryConfig, callable);
        assertThat(results.hasSucced()).isTrue();
    }

    @Test
    public void verifySimpleFixedProfile() {
        Callable<Boolean> callable = () -> true;

        RetryConfig<Boolean> retryConfig = retryConfigBuilder
            .fixedBackoff5Tries10Sec()
            .build();

        RetryStatus<Boolean> results = Retry.runSync(retryConfig, callable);
        assertThat(results.hasSucced()).isTrue();
    }
}