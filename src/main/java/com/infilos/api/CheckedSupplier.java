package com.infilos.api;

import com.infilos.utils.Require;

/** A supplier that can throw checked exceptions. */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Throwable> {
    T get() throws E;

    /**
     * Returns a new {@code CheckedSupplier} that maps the return value using {@code mapper}. For example: {@code (x -> 1).andThen(Object::toString).get() => "1"}.
     */
    default <R> CheckedSupplier<R, E> andThen(
        CheckedFunction<? super T, ? extends R, ? extends E> mapper) {
        Require.checkNotNull(mapper);
        return () -> mapper.apply(get());
    }
}
