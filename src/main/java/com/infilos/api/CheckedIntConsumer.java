package com.infilos.api;

import com.infilos.utils.Require;

@FunctionalInterface
public interface CheckedIntConsumer<E extends Throwable> {
    void accept(int input) throws E;

    /**
     * Returns a new {@code CheckedIntConsumer} that also passes the input to {@code that}. For example: {@code out::writeInt.andThen(logger::logInt).accept(123)}.
     */
    default CheckedIntConsumer<E> andThen(CheckedIntConsumer<E> that) {
        Require.checkNotNull(that);
        return input -> {
            accept(input);
            that.accept(input);
        };
    }
}
