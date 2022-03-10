package com.infilos.utils.retry;

import com.infilos.utils.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * @param <E> is the event type, throwed exception or returned value.
 */
public abstract class RetryDelay<E> implements Comparable<RetryDelay<E>>, Loggable {

    /**
     * The delay interval.
     */
    public abstract Duration duration();


    public static <E> RetryDelay<E> ofMillis(long millis) {
        return of(Duration.ofMillis(millis));
    }

    public static <E> RetryDelay<E> ofSeconds(long seconds) {
        return of(Duration.ofSeconds(seconds));
    }

    public static <E> RetryDelay<E> of(Duration duration) {
        requireNonNegative(duration);

        return new RetryDelay<E>() {
            @Override
            public Duration duration() {
                return duration;
            }
        };
    }

    /**
     * Returns a view of {@code list} that while not modifiable, will become empty when {@link #duration} has elapsed since the time the view was created as if another thread had just concurrently removed all elements from it.
     *
     * <p>Useful for setting a retry deadline to avoid long response time. For example:
     *
     * <pre>{@code
     *   Delay<?> deadline = Delay.ofMillis(500);
     *   new Retryer()
     *       .upon(RpcException.class,
     *             deadline.timed(Delay.ofMillis(30).exponentialBackoff(2, 5), clock))
     *       .retry(this::getAccount, executor);
     * }</pre>
     *
     * <p>The returned {@code List} view's state is dependent on the current time.
     * Beware of copying the list, because when you do, time is frozen as far as the copy is concerned. Passing the copy to {@link #devise upon()} no longer respects "timed" semantics.
     *
     * <p>Note that if the timed deadline <em>would have been</em> exceeded after the current
     * delay, that delay will be considered "removed" and hence cause the retry to stop.
     *
     * <p>{@code clock} is used to measure time.
     */
    public final <T extends RetryDelay<?>> List<T> timed(List<T> list, Clock clock) {
        Require.checkNotNull(list);

        Instant until = clock.instant().plus(duration());

        return new AbstractList<T>() {
            @Override
            public T get(int index) {
                T actual = list.get(index);
                if (clock.instant().plus(actual.duration()).isBefore(until)) {
                    return actual;
                }

                throw new IndexOutOfBoundsException();
            }

            @Override
            public int size() {
                return clock.instant().isBefore(until) ? list.size() : 0;
            }
        };
    }

    /**
     * Returns a view of {@code list} that while not modifiable, will become empty when {@link #duration} has elapsed since the time the view was created as if another thread had just concurrently removed all elements from it.
     *
     * <p>Useful for setting a retry deadline to avoid long response time. For example:
     *
     * <pre>{@code
     *   Delay<?> deadline = Delay.ofMillis(500);
     *   new Retryer()
     *       .upon(RpcException.class, deadline.timed(Delay.ofMillis(30).exponentialBackoff(2, 5)))
     *       .retry(this::getAccount, executor);
     * }</pre>
     *
     * <p>The returned {@code List} view's state is dependent on the current time.
     * Beware of copying the list, because when you do, time is frozen as far as the copy is concerned. Passing the copy to {@link #upon upon()} no longer respects "timed" semantics.
     *
     * <p>Note that if the timed deadline <em>would have been</em> exceeded after the current
     * delay, that delay will be considered "removed" and hence cause the retry to stop.
     */
    public final <T extends RetryDelay<?>> List<T> timed(List<T> list) {
        return timed(list, Clock.systemUTC());
    }

    /**
     * Returns an immutable {@code List} of delays with {@code size}. The first delay (if {@code size > 0}) is {@code this} and the following delays are exponentially multiplied using {@code multiplier}.
     *
     * @param multiplier must be positive
     * @param size       must not be negative
     */
    public final List<RetryDelay<E>> withBackoff(double multiplier, int size) {
        if (multiplier <= 0) throw new IllegalArgumentException("Invalid multiplier: " + multiplier);
        if (checkSize(size) == 0) return Collections.emptyList();
        
        return new AbstractList<RetryDelay<E>>() {
            @Override
            public RetryDelay<E> get(int index) {
                return multipliedBy(Math.pow(multiplier, checkIndex(index, size)));
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    /**
     * Returns a new {@code Delay} with duration multiplied by {@code multiplier}.
     *
     * @param multiplier must not be negative
     */
    public final RetryDelay<E> multipliedBy(double multiplier) {
        Require.check(multiplier >= 0, "Invalid multiplier: " + multiplier);

        return ofMillis(Math.round(Math.ceil(duration().toMillis() * multiplier)));
    }

    /**
     * Returns a new {@code Delay} with some extra randomness. To randomize a list of {@code Delay}s, for example:
     *
     * <pre>{@code
     *   Random random = new Random();
     *   List<Delay> randomized = Delay.ofMillis(100).exponentialBackoff(2, 5).stream()
     *       .map(d -> d.randomized(random, 0.5))
     *       .collect(toList());
     * }</pre>
     *
     * @param random     random generator
     * @param randomness Must be in the range of [0, 1]. 0 means no randomness; and 1 means the delay randomly ranges from 0x to 2x.
     */
    public final RetryDelay<E> randomized(Random random, double randomness) {
        Require.checkNotNull(random);
        if (randomness < 0 || randomness > 1) {
            throw new IllegalArgumentException("Randomness must be in range of [0, 1]: " + randomness);
        }
        if (randomness == 0) {
            return this;
        }

        return multipliedBy(1 + (random.nextDouble() - 0.5) * 2 * randomness);
    }

    /**
     * Returns a fibonacci list of delays of {@code size}, as in {@code 1, 1, 2, 3, 5, 8, ...} with {@code this} delay being the multiplier.
     */
    public final List<RetryDelay<E>> fibonacci(int size) {
        if (checkSize(size) == 0) {
            return Collections.emptyList();
        }
        
        return new AbstractList<RetryDelay<E>>() {
            @Override
            public RetryDelay<E> get(int index) {
                return ofMillis(Math.round(fib(checkIndex(index, size) + 1) * duration().toMillis()));
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    /** Called if {@code event} will be retried after the delay. Logs the event by default. */
    public void beforeDelay(E event) {
        log().info(event + ": will retry after " + duration());
    }

    /** Called after the delay, immediately before the retry. Logs the event by default. */
    public void afterDelay(E event) {
        log().info(event + ": " + duration() + " has passed. Retrying now...");
    }

    /** Called if delay for {@code event} is interrupted. */
    void interrupted(E event) {
        log().info(event + ": interrupted while waiting to retry upon .");
        Thread.currentThread().interrupt();
    }

    final void synchronously(E event) throws InterruptedException {
        beforeDelay(event);
        Thread.sleep(duration().toMillis());
        afterDelay(event);
    }

    final void asynchronously(E event, CatchedRunnable retry, ScheduledExecutorService executor, CompletableFuture<?> result) {
        beforeDelay(event);

        CatchedRunnable afterDelay = () -> {
            afterDelay(event);
            retry.run();
        };

        ScheduledFuture<?> scheduled = executor.schedule(
            () -> afterDelay.run(result::completeExceptionally),
            duration().toMillis(), TimeUnit.MILLISECONDS);

        Threads.ifCancelled(result, canceled -> {
            scheduled.cancel(true);
        });
    }

    /**
     * Returns an adapter of {@code this} as type {@code F}, which uses {@code eventTranslator} to translate events to type {@code E} before accepting them.
     */
    final <F> RetryDelay<F> forEvents(Function<F, ? extends E> eventTranslator) {
        Require.checkNotNull(eventTranslator);

        RetryDelay<E> delegate = this;
        return new RetryDelay<F>() {
            @Override
            public Duration duration() {
                return delegate.duration();
            }

            @Override
            public void beforeDelay(F from) {
                delegate.beforeDelay(eventTranslator.apply(from));
            }

            @Override
            public void afterDelay(F from) {
                delegate.afterDelay(eventTranslator.apply(from));
            }

            @Override
            void interrupted(F from) {
                delegate.interrupted(eventTranslator.apply(from));
            }
        };
    }

    @Override
    public int compareTo(RetryDelay<E> that) {
        return duration().compareTo(that.duration());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RetryDelay) {
            RetryDelay<?> that = (RetryDelay<?>) obj;
            return duration().equals(that.duration());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return duration().hashCode();
    }

    @Override
    public String toString() {
        return duration().toString();
    }

    static double fib(int n) {
        double phi = 1.6180339887;
        return (Math.pow(phi, n) - Math.pow(-phi, -n)) / (2 * phi - 1);
    }

    private static Duration requireNonNegative(Duration duration) {
        Require.check(duration.toMillis() >= 0, "Negative duration: " + duration);

        return duration;
    }

    private static int checkSize(int size) {
        Require.check(size >= 0, "Invalid size: " + size);
        
        return size;
    }

    private static int checkIndex(int index, int size) {
        Require.check(index >=0 && index < size, "Invalid index: " + index);
        
        return index;
    }
}
