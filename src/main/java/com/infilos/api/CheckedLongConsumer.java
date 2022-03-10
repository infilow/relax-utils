package com.infilos.api;

import com.infilos.utils.Require;

@FunctionalInterface
public interface CheckedLongConsumer<E extends Throwable> {
    void accept(long input) throws E;

    /**
     * Returns a new {@code CheckedLongConsumer} that also passes the input to {@code that}. For example: {@code out::writeLong.andThen(logger::logLong).accept(123L)}.
     */
    default CheckedLongConsumer<E> andThen(CheckedLongConsumer<E> that) {
        Require.checkNotNull(that);
        return input -> {
            accept(input);
            that.accept(input);
        };
    }
}
