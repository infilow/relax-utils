package com.infilos.api;

import com.infilos.utils.Require;

import java.util.Objects;
import java.util.function.Predicate;

public final class Predicates {
    private Predicates() {
    }

    /**
     * Wrap predicate with type check.
     */
    public static <T> Predicate<Object> ofTyped(Class<T> type, Predicate<? super T> condition) {
        Require.check(Objects.nonNull(type));
        Require.check(Objects.nonNull(condition));
        
        return x -> type.isInstance(x) && condition.test(type.cast(x));
    }
}
