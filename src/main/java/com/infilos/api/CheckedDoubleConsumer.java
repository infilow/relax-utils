package com.infilos.api;

import com.infilos.utils.Require;

@FunctionalInterface
public interface CheckedDoubleConsumer<E extends Throwable> {
    void accept(double input) throws E;

    /**
     * Returns a new {@code CheckedDoubleConsumer} that also passes the input to {@code that}. For example: {@code out::writeDouble.andThen(logger::logDouble).accept(123D)}.
     */
    default CheckedDoubleConsumer<E> andThen(CheckedDoubleConsumer<E> that) {
        Require.checkNotNull(that);
        return input -> {
            accept(input);
            that.accept(input);
        };
    }
}
