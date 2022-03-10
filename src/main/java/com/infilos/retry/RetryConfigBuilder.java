package com.infilos.retry;

import com.infilos.retry.backoff.*;
import com.infilos.retry.exception.RetryInvalidConfigException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * @param <T> is the return value type.
 */
public class RetryConfigBuilder<T> {

    final static String MUST_SPECIFY_BACKOFF
        = "Retry config must specify a backoff strategy!";
    final static String MUST_SPECIFY_MAX_ATTEMPT
        = "Retry config must specify a maximum number of tries!";
    final static String CAN_ONLY_SPECIFY_ONE_BACKOFF
        = "Retry config cannot specify more than one backoff strategy!";
    final static String ALREADY_SPECIFIED_ATTEMPT
        = "Number of tries can only be specified once!";
    final static String CAN_ONLY_SPECIFY_CUSTOM_EXCEPTION_STRAT
        = "You cannot use built in exception logic and custom exception logic in the same config!";
    final static String MUST_SPECIFY_MAX_TRIES_ABOVE_0
        = "Cannot specify a maximum number of attempt less than 1!";
    static final String SHOULD_SPECIFY_VALID_DELAY_DURATION
        = "Delay between retries must be a non-negative Duration.";

    private boolean enableValidation;
    private boolean builtInExceptionStrategySpecified;

    // retry depends on throwed error
    private Boolean retryOnAnyError = false;
    private final Set<Class<? extends Exception>> retryOnErrorIncluding = new HashSet<>();
    private final Set<Class<? extends Exception>> retryOnErrorExcluding = new HashSet<>();
    private Function<Exception, Boolean> retryOnErrorMatcher;
    private boolean retryOnErrorOfCausedBy = false;

    // retry depends on returned value
    private Boolean retryOnValue = false;
    private final Collection<T> retryOnValueIncluding = new ArrayList<>();
    private final Collection<T> retryOnValueExcluding = new ArrayList<>();
    private Function<T, Boolean> retryOnValueMatcher;

    // attempt/delay/backoff
    private Integer maxAttempts;
    private Duration delayDuration;
    private BackoffStrategy backoffStrategy;

    // listener
    private RetryListener<T> afterFailTryListener;
    private RetryListener<T> beforeNextTryListener;
    private RetryListener<T> onSuccessListener;
    private RetryListener<T> onFailureListener;
    private RetryListener<T> onCompletionListener;
    
    // async thread pool
    private ExecutorService executorService;

    public RetryConfigBuilder() {
        this.enableValidation = true;
        this.builtInExceptionStrategySpecified = false;
    }

    public RetryConfigBuilder(boolean enableValidation) {
        this();
        this.enableValidation = enableValidation;
    }

    public RetryConfigBuilder<T> enableValidation() {
        enableValidation = true;
        return this;
    }

    public RetryConfigBuilder<T> disableValidation() {
        enableValidation = false;
        return this;
    }

    public RetryConfigBuilder<T> retryOnErrorOfCausedBy() {
        retryOnErrorOfCausedBy = true;
        return this;
    }

    public RetryConfigBuilder<T> retryOnError(Class<? extends Exception> errorType) {
        retryOnErrorIncluding.add(errorType);
        return this;
    }

    public RetryConfigBuilder<T> retryOnErrors(List<Class<? extends Exception>> errorTypes) {
        retryOnErrorIncluding.addAll(errorTypes);
        builtInExceptionStrategySpecified = true;
        return this;
    }

    public RetryConfigBuilder<T> retryOnAnyError() {
        retryOnAnyError = true;
        builtInExceptionStrategySpecified = true;

        return this;
    }

    public RetryConfigBuilder<T> retryOnAnyErrorExclude(Class<? extends Exception> excludeErrorType) {
        retryOnErrorExcluding.add(excludeErrorType);
        builtInExceptionStrategySpecified = true;

        return this;
    }

    public RetryConfigBuilder<T> retryOnAnyErrorExclude(List<Class<? extends Exception>> excludeErrorTypes) {
        retryOnErrorExcluding.addAll(excludeErrorTypes);
        builtInExceptionStrategySpecified = true;

        return this;
    }

    public RetryConfigBuilder<T> retryOnErrorMatch(Function<Exception, Boolean> errorMatcher) {
        retryOnErrorMatcher = errorMatcher;
        return this;
    }

    public RetryConfigBuilder<T> failOnAnyError() {
        retryOnAnyError = false;
        retryOnErrorIncluding.clear();
        builtInExceptionStrategySpecified = true;

        return this;
    }

    public RetryConfigBuilder<T> retryOnValue(T value) {
        retryOnValue = true;
        retryOnValueIncluding.add(value);

        return this;
    }

    @SafeVarargs
    public final RetryConfigBuilder<T> retryOnValues(T... values) {
        retryOnValue = true;
        retryOnValueIncluding.addAll(Arrays.asList(values));

        return this;
    }

    @SafeVarargs
    public final RetryConfigBuilder<T> retryOnAnyValueExclude(T... excludeValues) {
        retryOnValue = true;
        retryOnValueExcluding.addAll(Arrays.asList(excludeValues));

        return this;
    }

    public RetryConfigBuilder<T> retryOnValueMatch(Function<T, Boolean> valueMatcher) {
        retryOnValue = true;
        retryOnValueMatcher = valueMatcher;
        return this;
    }

    public RetryConfigBuilder<T> withMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new RetryInvalidConfigException(MUST_SPECIFY_MAX_TRIES_ABOVE_0);
        }

        this.maxAttempts = maxAttempts;
        return this;
    }

    public RetryConfigBuilder<T> withInfiniteAttempts() {
        maxAttempts = Integer.MAX_VALUE;
        return this;
    }

    public RetryConfigBuilder<T> withDelayDuration(Duration delayDuration) {
        if (delayDuration.isNegative()) {
            throw new RetryInvalidConfigException(SHOULD_SPECIFY_VALID_DELAY_DURATION);
        }

        this.delayDuration = delayDuration;
        return this;
    }

    public RetryConfigBuilder<T> withDelayDuration(long amount, ChronoUnit unit) {
        return withDelayDuration(Duration.of(amount, unit));
    }


    public RetryConfigBuilder<T> withFixedBackoff() {
        validateBackoffStrategyAddition();
        backoffStrategy = new FixedBackoffStrategy();
        return this;
    }

    public RetryConfigBuilder<T> withExponentialBackoff() {
        validateBackoffStrategyAddition();
        backoffStrategy = new ExponentialBackoffStrategy();
        return this;
    }

    public RetryConfigBuilder<T> withFibonacciBackoff() {
        validateBackoffStrategyAddition();
        backoffStrategy = new FibonacciBackoffStrategy();
        return this;
    }

    public RetryConfigBuilder<T> withNoDelayBackoff() {
        validateBackoffStrategyAddition();
        backoffStrategy = new NoWaitBackoffStrategy();
        return this;
    }

    public RetryConfigBuilder<T> withRandomBackoff() {
        validateBackoffStrategyAddition();
        backoffStrategy = new RandomBackoffStrategy();
        return this;
    }

    public RetryConfigBuilder<T> withRandomExponentialBackoff() {
        validateBackoffStrategyAddition();
        backoffStrategy = new RandomExponentialBackoffStrategy();
        return this;
    }

    public RetryConfigBuilder<T> withCustomBackoff(BackoffStrategy backoffStrategy) {
        validateBackoffStrategyAddition();
        this.backoffStrategy = backoffStrategy;
        return this;
    }

    public RetryConfigBuilder<T> listenSuccedTry(RetryListener<T> listener) {
        this.onSuccessListener = listener;
        return this;
    }

    public RetryConfigBuilder<T> listenFailedTry(RetryListener<T> listener) {
        this.onFailureListener = listener;
        return this;
    }

    public RetryConfigBuilder<T> listenCompleteTry(RetryListener<T> listener) {
        this.onCompletionListener = listener;
        return this;
    }

    public RetryConfigBuilder<T> listenBeforeNextTry(RetryListener<T> listener) {
        this.beforeNextTryListener = listener;
        return this;
    }

    public RetryConfigBuilder<T> listenAfterFailTry(RetryListener<T> listener) {
        this.afterFailTryListener = listener;
        return this;
    }

    public RetryConfigBuilder<T> asyncThreadPool(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public RetryConfig<T> build() {
        RetryConfig<T> config = new RetryConfig<>(
            retryOnAnyError,
            retryOnErrorIncluding,
            retryOnErrorExcluding,
            retryOnErrorMatcher,
            retryOnErrorOfCausedBy,
            retryOnValue,
            retryOnValueIncluding,
            retryOnValueExcluding,
            retryOnValueMatcher,
            maxAttempts,
            delayDuration,
            backoffStrategy,
            afterFailTryListener,
            beforeNextTryListener,
            onSuccessListener,
            onFailureListener,
            onCompletionListener,
            executorService
        );

        validateConfig(config);

        return config;
    }

    private void validateConfig(RetryConfig<T> retryConfig) {
        if (!enableValidation) {
            return;
        }

        if (null == retryConfig.getBackoffStrategy()) {
            throw new RetryInvalidConfigException(MUST_SPECIFY_BACKOFF);
        }

        if (null == retryConfig.getMaxAttempts()) {
            throw new RetryInvalidConfigException(MUST_SPECIFY_MAX_ATTEMPT);
        }

        if (null != retryConfig.getRetryOnErrorMatcher() && builtInExceptionStrategySpecified) {
            throw new RetryInvalidConfigException(CAN_ONLY_SPECIFY_CUSTOM_EXCEPTION_STRAT);
        }

        backoffStrategy.checkConfig(retryConfig);
    }

    private void validateBackoffStrategyAddition() {
        if (!enableValidation) {
            return;
        }

        if (null != backoffStrategy) {
            throw new RetryInvalidConfigException(CAN_ONLY_SPECIFY_ONE_BACKOFF);
        }
    }

    public RetryConfigBuilder<T> fixedBackoff2Tries3Sec() {
        return this
            .retryOnAnyError()
            .withMaxAttempts(2)
            .withDelayDuration(3, SECONDS)
            .withFixedBackoff();
    }

    public RetryConfigBuilder<T> fixedBackoff5Tries10Sec() {
        return this
            .retryOnAnyError()
            .withMaxAttempts(5)
            .withDelayDuration(10, SECONDS)
            .withFixedBackoff();
    }

    public RetryConfigBuilder<T> exponentialBackoff5Tries5Sec() {
        return this
            .retryOnAnyError()
            .withMaxAttempts(5)
            .withDelayDuration(5, SECONDS)
            .withExponentialBackoff();
    }

    public RetryConfigBuilder<T> fiboBackoff7Tries5Sec() {
        return this
            .retryOnAnyError()
            .withMaxAttempts(7)
            .withDelayDuration(5, SECONDS)
            .withFibonacciBackoff();
    }

    public RetryConfigBuilder<T> randomExpBackoff10Tries60Sec() {
        return this
            .retryOnAnyError()
            .withMaxAttempts(10)
            .withDelayDuration(60, SECONDS)
            .withRandomExponentialBackoff();
    }
}
