package com.infilos.api;

/**
 * A binary predicate that can throw checked exceptions.
 */
@FunctionalInterface
public interface CheckedBiPredicate<A, B, E extends Throwable> {
    boolean test(A a, B b) throws E;
}
