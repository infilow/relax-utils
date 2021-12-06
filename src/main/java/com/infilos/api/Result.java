package com.infilos.api;

import com.infilos.utils.Require;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Aka scala's Result<T>.
 */
public interface Result<T> {

    boolean isSucced();

    void ifSucced(final Consumer<T> consumer);

    boolean isFailed();

    void ifFailed(final Consumer<Throwable> consumer);

    Result<T> switchIfFailed(final Function<Throwable, Result<T>> fallbackMethod);

    <U> Result<U> map(final Function<? super T, ? extends U> mapper);

    <U> Result<U> flatMap(final Function<? super T, Result<U>> mapper);

    Result<T> mapFalied(final Function<Throwable, ? extends Throwable> mapper);

    T get();

    T getOrElse(final Supplier<T> supplier);

    Throwable getFailure();

    // below are factories

    static <T> Result<T> succed(final T value) {
        Require.checkNotNull(value, "The value of a Result cannot be null");
        return new Succed<>(value);
    }

    static <T, E extends Throwable> Result<T> failed(final E throwable) {
        Require.checkNotNull(throwable, "The error of a Result cannot be null");
        return new Failed<>(throwable);
    }

    static <T> Result<T> of(final Supplier<T> supplier) {
        Require.checkNotNull(supplier, "The value supplier cannot be null");

        try {
            return succed(supplier.get());
        } catch (final Exception error) {
            return failed(error);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Result<T> of(final Optional<T> optional) {
        Require.checkNotNull(optional, "The optional value cannot be null");

        return optional
            .map(Result::succed)
            .orElseGet(() -> failed(new NoSuchElementException("No value present when unwrapping the optional")));
    }

    static <T> Result<T> ofNullable(final T value) {
        return ofNullable(value, () -> new NullPointerException("The result was initialized with a null value"));
    }

    static <T> Result<T> ofNullable(final T value, final Supplier<? extends Throwable> errorSupplier) {
        Require.checkNotNull(errorSupplier, "The error supplier cannot be null");

        return Objects.nonNull(value)
            ? succed(value)
            : failed(errorSupplier.get());
    }
}
