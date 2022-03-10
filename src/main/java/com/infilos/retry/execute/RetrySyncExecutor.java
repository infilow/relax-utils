package com.infilos.retry.execute;

import com.infilos.retry.*;
import com.infilos.retry.RetryConfig;
import com.infilos.retry.exception.RetryEscapedException;
import com.infilos.retry.exception.RetryTiredException;
import com.infilos.utils.Loggable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation that does a single, synchronous retry in the same thread that it is called from.
 *
 * @param <T> The type that is returned by the Callable (eg: Boolean, Void, Object, etc)
 */
public class RetrySyncExecutor<T> implements RetryExecutor<T, RetryStatus<T>>, Loggable {

    private final RetryConfig<T> config;

    private final RetryStatus<T> finalStatus = new RetryStatus<>();
    private T lastValueCausedRetry = null;
    private Exception lastErrorCausedRetry = null;

    public RetrySyncExecutor(RetryConfig<T> config) {
        this.config = config;
    }

    @Override
    public RetryStatus<T> execute(Callable<T> callable) {
        return execute(callable, null);
    }

    @Override
    public RetryStatus<T> execute(Callable<T> callable, String operation) {
        log().trace("Starting retry execution with callable {} {} {}", config, operation, callable);
        log().debug("Starting retry execution with executor state {}", this);

        this.finalStatus.setOperation(operation);
        this.finalStatus.setStartTime(System.currentTimeMillis());

        int maxAttempts = config.getMaxAttempts();
        long delay = Objects.nonNull(config.getDelayDuration()) ? config.getDelayDuration().toMillis() : 0L;

        AttemptStatus<T> attemptStatus = new AttemptStatus<>();
        attemptStatus.setSucced(false);

        int currentAttempt;

        try {
            for (currentAttempt = 0; currentAttempt < maxAttempts && !attemptStatus.hasSucced(); currentAttempt++) {
                if (currentAttempt > 0) {
                    handleBeforeNextTry(delay, currentAttempt);
                    log().trace("retry retrying for time number {}", currentAttempt);
                }

                log().trace("retry executing callable {}", callable);
                attemptStatus = tryCall(callable);

                if (!attemptStatus.hasSucced()) {
                    handleFailedTry(currentAttempt + 1);
                }
            }

            collectFinalStatus(attemptStatus.hasSucced(), currentAttempt);
            this.finalStatus.setFinishTime(System.currentTimeMillis());

            postExecutionCleanup(callable, maxAttempts, attemptStatus);

            log().debug("Finished retry execution in {} ms", finalStatus.getTotalDuration().toMillis());
            log().trace("Finished retry execution with executor state {}", this);
        } finally {
            if (null != config.getOnCompletionListener()) {
                config.getOnCompletionListener().onEvent(finalStatus);
            }
        }

        return finalStatus;
    }

    private void postExecutionCleanup(Callable<T> callable, int maxTries, AttemptStatus<T> attemptStatus) {
        if (!attemptStatus.hasSucced()) {
            String failure = String.format("Execute '%s' failed after %d tries!", callable.toString(), maxTries);
            if (null != config.getOnFailureListener()) {
                config.getOnFailureListener().onEvent(finalStatus);
            } else {
                log().trace("Throwing retries tired exception");
                throw new RetryTiredException(failure, lastErrorCausedRetry, finalStatus);
            }
        } else {
            finalStatus.setResult(attemptStatus.getResult());
            if (null != config.getOnSuccessListener()) {
                config.getOnSuccessListener().onEvent(finalStatus);
            }
        }
    }

    private AttemptStatus<T> tryCall(Callable<T> callable) throws RetryEscapedException {
        AttemptStatus<T> attemptStatus = new AttemptStatus<>();

        try {
            T callResult = callable.call();

            //boolean shouldRetryOnResult = config.shouldRetryOnValue() && (
            //    (!config.getRetryOnValueExcluding().isEmpty() && !isOneOfRetryOnValueExcluding(callResult))
            //        || isOneOfRetryOnValueIncluding(callResult)
            //);
            if (checkShouldRetryOnResult(callResult)) {
                lastValueCausedRetry = callResult;
                attemptStatus.setSucced(false);
            } else {
                attemptStatus.setResult(callResult);
                attemptStatus.setSucced(true);
            }
        } catch (Exception e) {
            if (shouldThrowException(e)) {
                log().trace("Throwing expected exception", e);
                throw new RetryEscapedException("Unexpected exception thrown during retry execution!", e);
            } else {
                lastErrorCausedRetry = e;
                attemptStatus.setSucced(false);
            }
        }

        return attemptStatus;
    }
    
    private boolean checkShouldRetryOnResult(T callResult) {
        if (!config.shouldRetryOnValue()) {
            return false;
        }
        if(!config.getRetryOnValueExcluding().isEmpty() && !isOneOfRetryOnValueExcluding(callResult)) {
            return true;
        }
        if(isOneOfRetryOnValueIncluding(callResult)) {
            return true;
        }
        if(Objects.nonNull(config.getRetryOnValueMatcher())) {
            try {
                return config.getRetryOnValueMatcher().apply(callResult);
            } catch (Throwable ignored){
            }
        }
        
        return false;
    }

    private boolean isOneOfRetryOnValueExcluding(T callResult) {
        Collection<T> valuesToExpect = config.getRetryOnValueExcluding();
        if (valuesToExpect != null) {
            for (T o : valuesToExpect) {
                if (o.equals(callResult)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOneOfRetryOnValueIncluding(T callResult) {
        Collection<T> valuesToRetryOn = config.getRetryOnValueIncluding();
        if (valuesToRetryOn != null) {
            for (Object o : valuesToRetryOn) {
                if (o.equals(callResult)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleBeforeNextTry(final long delayMills, final int tries) {
        sleep(delayMills, tries);
        if (null != config.getBeforeNextTryListener()) {
            config.getBeforeNextTryListener().onEvent(finalStatus);
        }
    }

    private void handleFailedTry(int tries) {
        collectFinalStatus(false, tries);

        if (null != config.getAfterFailTryListener()) {
            config.getAfterFailTryListener().onEvent(finalStatus);
        }
    }

    private void collectFinalStatus(boolean success, int tries) {
        long current = System.currentTimeMillis();
        long elapsed = current - finalStatus.getStartTime();

        finalStatus.setTotalTries(tries);
        finalStatus.setTotalDuration(Duration.of(elapsed, ChronoUnit.MILLIS));
        finalStatus.setSucced(success);
        finalStatus.setLastException(lastErrorCausedRetry);
    }

    private void sleep(long millis, int tries) {
        Duration duration = Duration.of(millis, ChronoUnit.MILLIS);
        long delayMills = config.getBackoffStrategy()
            .nextDelayToWait(tries, duration, lastValueCausedRetry, lastErrorCausedRetry)
            .toMillis();

        log().trace("retry executor sleeping for {} ms", delayMills);
        try {
            TimeUnit.MILLISECONDS.sleep(delayMills);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean shouldThrowException(Exception e) {
        if (this.config.getRetryOnErrorMatcher() != null) {
            //custom retry logic
            return !this.config.getRetryOnErrorMatcher().apply(e);
        } else {
            //config says to always retry
            if (this.config.shouldRetryOnAnyError()) {
                return false;
            }

            Set<Class<?>> exceptionsToMatch = new HashSet<>();
            exceptionsToMatch.add(e.getClass());
            if (this.config.shouldRetryOnErrorOfCausedBy()) {
                exceptionsToMatch.clear();
                exceptionsToMatch.addAll(getExceptionCauses(e));
            }

            return exceptionsToMatch.stream().noneMatch(this::matchesException);
        }
    }

    private boolean matchesException(Class<?> thrownExceptionClass) {
        //config says to retry only on specific exceptions
        for (Class<? extends Exception> exceptionToRetryOn : this.config.getRetryOnErrorIncluding()) {
            if (exceptionToRetryOn.isAssignableFrom(thrownExceptionClass)) {
                return true;
            }
        }

        //config says to retry on all except specific exceptions
        if (!this.config.getRetryOnErrorExcluding().isEmpty()) {
            for (Class<? extends Exception> exceptionToNotRetryOn : this.config.getRetryOnErrorExcluding()) {
                if (exceptionToNotRetryOn.isAssignableFrom(thrownExceptionClass)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private Set<Class<?>> getExceptionCauses(Exception exception) {
        Throwable parent = exception;
        Set<Class<?>> causes = new HashSet<>();
        while (parent.getCause() != null) {
            causes.add(parent.getCause().getClass());
            parent = parent.getCause();
        }
        return causes;
    }

    public RetryConfig<T> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CallExecutor{");
        sb.append("config=").append(config);
        sb.append(", lastKnownExceptionThatCausedRetry=").append(lastErrorCausedRetry);
        sb.append(", status=").append(finalStatus);
        sb.append('}');
        
        return sb.toString();
    }
}
