package com.infilos.api;

import com.infilos.utils.Require;

/** A binary consumer that can throw checked exceptions. */
@FunctionalInterface
public interface CheckedBiConsumer<A, B, E extends Throwable> {
    void accept(A a, B b) throws E;

    /**
     * Returns a new {@code CheckedBiConsumer} that also passes the inputs to {@code that}.
     */
    default <R> CheckedBiConsumer<A, B, E> andThen(
        CheckedBiConsumer<? super A, ? super B, ? extends E> that) {
        Require.checkNotNull(that);
        return (a, b) -> {
            accept(a, b);
            that.accept(a, b);
        };
    }
}
