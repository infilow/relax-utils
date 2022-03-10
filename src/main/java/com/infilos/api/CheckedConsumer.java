package com.infilos.api;

import com.infilos.utils.Require;

/** A consumer that can throw checked exceptions. */
@FunctionalInterface
public interface CheckedConsumer<T, E extends Throwable> {
    void accept(T input) throws E;

    /**
     * Returns a new {@code CheckedConsumer} that also passes the input to {@code that}. For example: {@code out::writeObject.andThen(logger::info).accept("message")}.
     */
    default CheckedConsumer<T, E> andThen(CheckedConsumer<? super T, E> that) {
        Require.checkNotNull(that);
        return input -> {
            accept(input);
            that.accept(input);
        };
    }
}
