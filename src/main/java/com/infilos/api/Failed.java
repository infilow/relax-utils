package com.infilos.api;

import com.infilos.utils.Require;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Failed<T> implements Result<T> {
    private final Throwable throwable;

    Failed(Throwable throwable) {
        this.throwable = throwable;
    }

    @SuppressWarnings("unchecked")
    private <E extends Throwable> T propagate(final Throwable throwable) throws E {
        throw (E) throwable;
    }

    @Override
    public boolean isSucced() {
        return false;
    }

    @Override
    public void ifSucced(Consumer<T> consumer) {
        // Do nothing when trying to consume the value of an Error result.
    }

    @Override
    public boolean isFailed() {
        return true;
    }

    @Override
    public void ifFailed(Consumer<Throwable> consumer) {
        Require.checkNotNull(consumer, "The error consumer cannot be null");
        consumer.accept(throwable);
    }

    @Override
    public Result<T> switchIfFailed(Function<Throwable, Result<T>> fallbackMethod) {
        Require.checkNotNull(fallbackMethod, "The fallback method cannot be null");
        return fallbackMethod.apply(throwable);
    }

    @Override
    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        return new Failed<>(throwable);
    }

    @Override
    public <U> Result<U> flatMap(Function<? super T, Result<U>> mapper) {
        return new Failed<>(throwable);
    }

    @Override
    public Result<T> mapFalied(Function<Throwable, ? extends Throwable> mapper) {
        Require.checkNotNull(mapper, "The error mapper cannot be null");
        return new Failed<>(mapper.apply(throwable));
    }

    @Override
    public T get() {
        return propagate(throwable);
    }

    @Override
    public T getOrElse(Supplier<T> supplier) {
        Require.checkNotNull(supplier);
        return supplier.get();
    }

    @Override
    public Throwable getFailure() {
        return throwable;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Failed<?> error = (Failed<?>) o;
        return Objects.equals(throwable, error.throwable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(throwable);
    }

    @Override
    public String toString() {
        return "Failed(" + "throwable=" + throwable + ')';
    }
}
