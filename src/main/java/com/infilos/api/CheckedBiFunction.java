package com.infilos.api;

import com.infilos.utils.Require;

/** A binary function that can throw checked exceptions. */
@FunctionalInterface
public interface CheckedBiFunction<A, B, T, E extends Throwable> {
    T apply(A a, B b) throws E;

    /**
     * Returns a new {@code CheckedBiFunction} that maps the return value using {@code mapper}. For example: {@code ((a, b) -> a + b).andThen(Object::toString).apply(1, 2) => "3"}.
     */
    default <R> CheckedBiFunction<A, B, R, E> andThen(
        CheckedFunction<? super T, ? extends R, ? extends E> mapper) {
        Require.checkNotNull(mapper);
        return (a, b) -> mapper.apply(apply(a, b));
    }
}