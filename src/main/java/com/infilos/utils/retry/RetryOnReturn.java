package com.infilos.utils.retry;

import com.infilos.api.CheckedSupplier;
import com.infilos.api.Maybe;
import com.infilos.utils.*;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Trigger retry depends on the returned value.
 */
public class RetryOnReturn<T> {
    private final RetryRunner retryRunner;
    private final Predicate<? super T> condition;

    RetryOnReturn(RetryRunner retryRunner,
                  Predicate<? super T> condition, 
                  List<? extends RetryDelay<? super T>> delays) {
        this.condition = Require.checkNotNull(condition);
        this.retryRunner = retryRunner.upon(
            ThrownReturn.class,
            // Safe because it's essentially ThrownReturn<T> and Delay<? super T>.
            mapList(delays, d -> d.forEvents(ThrownReturn::unsafeGet)));
    }

    /**
     * Invokes and possibly retries {@code supplier} according to the retry strategies specified with {@link #uponReturn uponReturn()}.
     *
     * <p>This method blocks while waiting to retry. If interrupted, retry is canceled.
     *
     * <p>If {@code supplier} fails despite retrying, the return value from the most recent
     * invocation is returned.
     */
    public <R extends T, E extends Throwable> R retryBlockingly(CheckedSupplier<R, E> supplier) throws E {
        return ThrownReturn.<R, E>unwrap(() -> retryRunner.retryBlockingly(supplier.andThen(this::wrap)));
    }

    /**
     * Invokes and possibly retries {@code supplier} according to the retry strategies specified with {@link #uponReturn uponReturn()}.
     *
     * <p>The first invocation is done in the current thread. Unchecked exceptions thrown by
     * {@code supplier} directly are propagated. This is to avoid hiding programming errors. Checked exceptions are reported through the returned {@link CompletionStage} so callers only need to deal with them in one place.
     *
     * <p>Retries are scheduled and performed by {@code executor}.
     *
     * <p>Canceling the returned future object will cancel currently pending retry attempts. Same
     * if {@code supplier} throws {@link InterruptedException}.
     *
     * <p>NOTE that if {@code executor.shutdownNow()} is called, the returned
     * {@link CompletionStage} will never be done.
     */
    public <R extends T, E extends Throwable> CompletionStage<R> retry(
        CheckedSupplier<? extends R, E> supplier,
        ScheduledExecutorService retryExecutor) {
        return ThrownReturn.unwrapAsync(() -> retryRunner.retry(supplier.andThen(this::wrap), retryExecutor));
    }

    /**
     * Invokes and possibly retries {@code asyncSupplier} according to the retry strategies specified with {@link #uponReturn uponReturn()}.
     *
     * <p>The first invocation is done in the current thread. Unchecked exceptions thrown by
     * {@code asyncSupplier} directly are propagated. This is to avoid hiding programming errors. Checked exceptions are reported through the returned {@link CompletionStage} so callers only need to deal with them in one place.
     *
     * <p>Retries are scheduled and performed by {@code executor}.
     *
     * <p>Canceling the returned future object will cancel currently pending retry attempts. Same
     * if {@code supplier} throws {@link InterruptedException}.
     *
     * <p>NOTE that if {@code executor.shutdownNow()} is called, the returned
     * {@link CompletionStage} will never be done.
     */
    public <R extends T, E extends Throwable> CompletionStage<R> retryAsync(
        CheckedSupplier<? extends CompletionStage<R>, E> asyncSupplier,
        ScheduledExecutorService retryExecutor) {
        return ThrownReturn.unwrapAsync(
            () -> retryRunner.retryAsync(() -> asyncSupplier.get().thenApply(this::wrap), retryExecutor));
    }

    private <R extends T> R wrap(R returnValue) {
        if (condition.test(returnValue)) {
            throw new ThrownReturn(returnValue);
        }
        
        return returnValue;
    }

    /**
     * This would have been static type safe if exception classes are allowed to be parameterized. Failing that, we have to resort to old time Object.
     *
     * At call site, we always wrap and unwrap in the same function for the same T, so we are safe.
     */
    @SuppressWarnings("serial")
    static final class ThrownReturn extends Error {
        private static final boolean DISABLE_SUPPRESSION = false;
        private static final boolean NO_STACK_TRACE = false;
        private final Object returnValue;

        ThrownReturn(Object returnValue) {
            super("This should never escape!", null, DISABLE_SUPPRESSION, NO_STACK_TRACE);
            this.returnValue = returnValue;
        }

        static <T, E extends Throwable> T unwrap(CheckedSupplier<T, E> supplier) throws E {
            try {
                return supplier.get();
            } catch (ThrownReturn thrown) {
                return thrown.unsafeGet();
            }
        }

        static <T, E extends Throwable> CompletionStage<T> unwrapAsync(CheckedSupplier<? extends CompletionStage<T>, E> supplier) throws E {
            CompletionStage<T> stage = unwrap(supplier);
            CompletionStage<T> outer = Maybe
                .catchException(ThrownReturn.class, stage)
                .thenApply(maybe -> maybe.getOrMap(ThrownReturn::unsafeGet));
            Threads.propagateCancellation(outer, stage);
            
            return outer;
        }

        /** Exception cannot be parameterized. But we essentially use it as ThrownReturn<T>. */
        @SuppressWarnings("unchecked")
        private <T> T unsafeGet() {
            return (T) returnValue;
        }
    }

    /** Only need it because we don't have Guava Lists.transform(). */
    private static <F, T> List<T> mapList(List<F> list, Function<? super F, ? extends T> mapper) {
        Require.checkNotNull(list);
        Require.checkNotNull(mapper);
        
        return new AbstractList<T>() {
            @Override
            public int size() {
                return list.size();
            }

            @Override
            public T get(int index) {
                return mapper.apply(list.get(index));
            }
        };
    }
}
