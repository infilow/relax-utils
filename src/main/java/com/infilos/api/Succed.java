package com.infilos.api;

import com.infilos.utils.Require;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Succed<T> implements Result<T>{
    private final T value;

    Succed(T value) {
        this.value = value;
    }

    @Override
    public boolean isSucced() {
        return true;
    }

    @Override
    public void ifSucced(Consumer<T> consumer) {
        Require.checkNotNull(consumer, "The value consumer cannot be null");
        consumer.accept(value);
    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public void ifFailed(Consumer<Throwable> consumer) {
        // Do nothing when trying to consume the error of an Ok result.
    }

    @Override
    public Result<T> switchIfFailed(Function<Throwable, Result<T>> fallbackMethod) {
        return new Succed<>(value);
    }

    @Override
    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        Require.checkNotNull(mapper, "The value mapper cannot be null");
        return new Succed<>(mapper.apply(value));
    }

    @Override
    public <U> Result<U> flatMap(Function<? super T, Result<U>> mapper) {
        Require.checkNotNull(mapper, "The value flat-mapper cannot be null");
        return mapper.apply(value);
    }

    @Override
    public Result<T> mapFalied(Function<Throwable, ? extends Throwable> mapper) {
        return new Succed<>(value);
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public T getOrElse(Supplier<T> supplier) {
        return value;
    }

    @Override
    public Throwable getFailure() {
        throw new NoSuchElementException("Result contains a value: " + value.toString());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Succed<?> ok = (Succed<?>) o;
        return Objects.equals(value, ok.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Succed(" + "value=" + value + ')';
    }
}
