package com.infilos.api;

import com.infilos.utils.Require;

/**
 * A function that can throw checked exceptions.
 */
@FunctionalInterface
public interface CheckedFunction<F, T, E extends Throwable> {
    T apply(F from) throws E;

    /**
     * Returns a new {@code CheckedFunction} that maps the return value using {@code mapper}. For example: {@code (x -> x).andThen(Object::toString).apply(1) => "1"}.
     */
    default <R> CheckedFunction<F, R, E> andThen(CheckedFunction<? super T, ? extends R, ? extends E> mapper) {
        Require.checkNotNull(mapper);
        
        return f -> mapper.apply(apply(f));
    }
}

