package com.infilos.utils.retry;

import com.infilos.api.CheckedSupplier;
import com.infilos.api.Maybe;
import com.infilos.utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

public class RetryRunner implements Loggable {

    private final RetryPlan<RetryDelay<?>> plan;

    /**
     * Create new RetryRunner with empty plan.
     */
    public RetryRunner() {
        this(new RetryPlan<>());
    }

    private RetryRunner(RetryPlan<RetryDelay<?>> plan) {
        this.plan = plan;
    }

    public final <E extends Throwable> RetryRunner upon(Class<E> exceptionType,
                                                        List<? extends RetryDelay<? super E>> delays) {
        return new RetryRunner(plan.devise(rejectInterruptedException(exceptionType), delays));
    }

    /**
     * Returns a new {@code Retryer} that uses {@code delays} when an exception is instance of {@code exceptionType}.
     *
     * <p>{@link InterruptedException} is always considered a request to stop retrying. Calling
     * {@code upon(InterruptedException.class, ...)} is illegal.
     */
    public final <E extends Throwable> RetryRunner upon(Class<E> exceptionType,
                                                        Stream<? extends RetryDelay<? super E>> delays) {
        return upon(exceptionType, copyOf(delays));
    }

    /**
     * Returns a new {@code Retryer} that uses {@code delays} when an exception is instance of {@code exceptionType} and satisfies {@code condition}.
     *
     * <p>{@link InterruptedException} is always considered a request to stop retrying. Calling
     * {@code upon(InterruptedException.class, ...)} is illegal.
     */
    public <E extends Throwable> RetryRunner upon(Class<E> exceptionType,
                                                  Predicate<? super E> condition,
                                                  List<? extends RetryDelay<? super E>> delays) {
        return new RetryRunner(plan.devise(rejectInterruptedException(exceptionType), condition, delays));
    }

    /**
     * Returns a new {@code Retryer} that uses {@code delays} when an exception is instance of {@code exceptionType} and satisfies {@code condition}.
     *
     * <p>{@link InterruptedException} is always considered a request to stop retrying. Calling
     * {@code upon(InterruptedException.class, ...)} is illegal.
     */
    public <E extends Throwable> RetryRunner upon(Class<E> exceptionType,
                                                  Predicate<? super E> condition,
                                                  Stream<? extends RetryDelay<? super E>> delays) {
        return upon(exceptionType, condition, copyOf(delays));
    }

    /**
     * Invokes and possibly retries {@code supplier} upon exceptions, according to the retry strategies specified with {@link #upon upon()}.
     *
     * <p>This method blocks while waiting to retry. If interrupted, retry is canceled.
     *
     * <p>If {@code supplier} fails despite retrying, the exception from the most recent invocation
     * is propagated.
     */
    public <T, E extends Throwable> T retryBlockingly(CheckedSupplier<T, E> supplier) throws E {
        Require.checkNotNull(supplier);

        List<Throwable> exceptions = new ArrayList<>();
        try {
            for (RetryPlan<RetryDelay<?>> currentPlan = plan; ; ) {
                try {
                    return supplier.get();
                } catch (Throwable e) {
                    if (e instanceof InterruptedException) throw e;
                    exceptions.add(e);
                    currentPlan = delay(e, currentPlan);
                }
            }
        } catch (Throwable e) {
            for (Throwable t : exceptions) addSuppressedTo(e, t);
            @SuppressWarnings("unchecked")  // Caller makes sure the exception is either E or unchecked.
            E checked = (E) Throws.propagateIfUnchecked(e);
            throw checked;
        }
    }

    /**
     * Invokes and possibly retries {@code supplier} upon exceptions, according to the retry strategies specified with {@link #upon upon()}.
     *
     * <p>The first invocation is done in the current thread. Unchecked exceptions thrown by
     * {@code supplier} directly are propagated unless explicitly configured to retry. This is to avoid hiding programming errors. Checked exceptions are reported through the returned {@link CompletionStage} so callers only need to deal
     * with them in one place.
     *
     * <p>Retries are scheduled and performed by {@code executor}.
     *
     * <p>Canceling the returned future object will cancel currently pending retry attempts. Same
     * if {@code supplier} throws {@link InterruptedException}.
     *
     * <p>NOTE that if {@code executor.shutdownNow()} is called, the returned {@link CompletionStage}
     * will never be done.
     */
    public <T> CompletionStage<T> retry(CheckedSupplier<T, ?> supplier,
                                        ScheduledExecutorService executor) {
        return retryAsync(supplier.andThen(CompletableFuture::completedFuture), executor);
    }


    /**
     * Invokes and possibly retries {@code asyncSupplier} upon exceptions, according to the retry strategies specified with {@link #upon upon()}.
     *
     * <p>The first invocation is done in the current thread. Unchecked exceptions thrown by
     * {@code asyncSupplier} directly are propagated unless explicitly configured to retry. This is to avoid hiding programming errors. Checked exceptions are reported through the returned {@link CompletionStage} so callers only need to
     * deal with them in one place.
     *
     * <p>Retries are scheduled and performed by {@code executor}.
     *
     * <p>Canceling the returned future object will cancel currently pending retry attempts. Same
     * if {@code supplier} throws {@link InterruptedException}.
     *
     * <p>NOTE that if {@code executor.shutdownNow()} is called, the returned {@link CompletionStage}
     * will never be done.
     */
    public <T> CompletionStage<T> retryAsync(CheckedSupplier<? extends CompletionStage<T>, ?> asyncSupplier,
                                             ScheduledExecutorService executor) {
        Require.checkNotNull(asyncSupplier);
        Require.checkNotNull(executor);
        CompletableFuture<T> future = new CompletableFuture<>();
        invokeWithRetry(asyncSupplier, executor, future);

        return future;
    }

    /**
     * Returns a new object that retries if the return value satisfies {@code condition}. {@code delays} specify the backoffs between retries.
     */
    public <T> RetryOnReturn<T> ifReturns(
        Predicate<T> condition, List<? extends RetryDelay<? super T>> delays) {
        return new RetryOnReturn<>(this, condition, delays);
    }

    /**
     * Returns a new object that retries if the return value satisfies {@code condition}. {@code delays} specify the backoffs between retries.
     */
    public <T> RetryOnReturn<T> ifReturns(
        Predicate<T> condition, Stream<? extends RetryDelay<? super T>> delays) {
        return ifReturns(condition, copyOf(delays));
    }

    /**
     * Returns a new object that retries if the function returns {@code returnValue}.
     *
     * @param returnValue The nullable return value that triggers retry
     * @param delays      specify the backoffs between retries
     */
    public <T> RetryOnReturn<T> uponReturn(
        T returnValue, Stream<? extends RetryDelay<? super T>> delays) {
        return uponReturn(returnValue, copyOf(delays));
    }

    /**
     * Returns a new object that retries if the function returns {@code returnValue}.
     *
     * @param returnValue The nullable return value that triggers retry
     * @param delays      specify the backoffs between retries
     */
    public <T> RetryOnReturn<T> uponReturn(
        T returnValue, List<? extends RetryDelay<? super T>> delays) {
        return ifReturns(r -> Objects.equals(r, returnValue), delays);
    }

    private <T> void invokeWithRetry(
        CheckedSupplier<? extends CompletionStage<T>, ?> supplier,
        ScheduledExecutorService retryExecutor,
        CompletableFuture<T> future) {
        if (future.isDone()) return;  // like, canceled before retrying.
        try {
            CompletionStage<T> stage = supplier.get();
            stage.handle((v, e) -> {
                if (e == null) future.complete(v);
                else scheduleRetry(getInterestedException(e), retryExecutor, supplier, future);
                return null;
            });
        } catch (RuntimeException e) {
            retryIfCovered(e, retryExecutor, supplier, future);
        } catch (Error e) {
            retryIfCovered(e, retryExecutor, supplier, future);
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                CancellationException cancelled = new CancellationException();
                cancelled.initCause(e);
                Thread.currentThread().interrupt();
                // Don't even attempt to retry, even if user explicitly asked to retry on Exception
                // This is because we treat InterruptedException specially as a signal to stop.
                throw cancelled;
            }
            scheduleRetry(e, retryExecutor, supplier, future);
        }
    }

    private <E extends Throwable, T> void retryIfCovered(
        E e, ScheduledExecutorService retryExecutor,
        CheckedSupplier<? extends CompletionStage<T>, ?> supplier, CompletableFuture<T> future)
        throws E {
        if (plan.anyMatches(e)) {
            scheduleRetry(e, retryExecutor, supplier, future);
        } else {
            throw e;
        }
    }

    private <T> void scheduleRetry(
        Throwable e, ScheduledExecutorService retryExecutor,
        CheckedSupplier<? extends CompletionStage<T>, ?> supplier, CompletableFuture<T> future) {
        try {
            Maybe<RetryExecution<RetryDelay<?>>, ?> maybeRetry = plan.execute(e);
            maybeRetry.ifPresent(execution -> {
                future.exceptionally(x -> {
                    addSuppressedTo(x, e);
                    return null;
                });
                if (future.isDone()) return;  // like, canceled immediately before scheduling.
                @SuppressWarnings("unchecked")  // delay came from upon(), which enforces <? super E>.
                RetryDelay<Throwable> delay = (RetryDelay<Throwable>) execution.strategy();
                RetryRunner nextRound = new RetryRunner(execution.remainingRetryPlan());
                CatchedRunnable retry = () -> nextRound.invokeWithRetry(supplier, retryExecutor, future);

                delay.asynchronously(e, retry, retryExecutor, future);
            });
            maybeRetry.onFailure(future::completeExceptionally);
        } catch (Throwable unexpected) {
            addSuppressedTo(unexpected, e);
            throw unexpected;
        }
    }


    private static <E extends Throwable> RetryPlan<RetryDelay<?>> delay(
        E exception, RetryPlan<RetryDelay<?>> plan) throws E {
        RetryExecution<RetryDelay<?>> execution = plan.execute(exception).getOrThrow(identity());
        @SuppressWarnings("unchecked")  // Applicable delays were from upon(), enforcing <? super E>
        RetryDelay<? super E> delay = (RetryDelay<? super E>) execution.strategy();
        try {
            delay.synchronously(exception);
        } catch (InterruptedException e) {
            delay.interrupted(exception);
            throw exception;
        }

        return execution.remainingRetryPlan();
    }

    private static <E extends Throwable> Class<E> rejectInterruptedException(Class<E> exceptionType) {
        if (InterruptedException.class.isAssignableFrom(exceptionType)) {
            throw new IllegalArgumentException("Cannot retry on InterruptedException.");
        }
        return exceptionType;
    }

    private static void addSuppressedTo(Throwable exception, Throwable suppressed) {
        if (suppressed instanceof RetryOnReturn.ThrownReturn) return;
        if (exception != suppressed) {  // In case user code throws same exception again.
            exception.addSuppressed(suppressed);
        }
    }

    private static Throwable getInterestedException(Throwable exception) {
        if (exception instanceof CompletionException || exception instanceof ExecutionException) {
            return exception.getCause() == null ? exception : exception.getCause();
        }
        return exception;
    }

    private static <T> List<T> copyOf(Stream<? extends T> stream) {
        // Collectors.toList() doesn't guarantee thread-safety.
        return stream.collect(Collectors.toCollection(ArrayList::new));
    }
}
