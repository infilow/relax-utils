package com.infilos.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

/**
 * <pre>{@code
 * Friend friend = Builder.of(Friend::new)
 *   .with(Friend::setName, "name")
 *   .with(Friend::setAge, 22)
 *   .build();
 * }</pre>
 */
public class Builder<T> {
    private final Supplier<T> instantiator;
    private final List<Consumer<T>> applySetters = new ArrayList<>();

    public Builder(Supplier<T> instantiator) {
        this.instantiator = instantiator;
    }

    public static <T> Builder<T> of(Supplier<T> instantiator) {
        return new Builder<>(instantiator);
    }

    public <P1> Builder<T> with(BiConsumer<T, P1> setter, P1 p1) {
        Consumer<T> applySetter = instance -> setter.accept(instance, p1);
        applySetters.add(applySetter);

        return this;
    }

    public <P1, P2> Builder<T> with(Consumer2<T, P1, P2> setter, P1 p1, P2 p2) {
        Consumer<T> applySetter = instance -> setter.accept(instance, p1, p2);
        applySetters.add(applySetter);

        return this;
    }

    public <P1, P2, P3> Builder<T> with(Consumer3<T, P1, P2, P3> setter, P1 p1, P2 p2, P3 p3) {
        Consumer<T> applySetter = instance -> setter.accept(instance, p1, p2, p3);
        applySetters.add(applySetter);

        return this;
    }

    public <P1, P2, P3, P4> Builder<T> with(Consumer4<T, P1, P2, P3, P4> setter, P1 p1, P2 p2, P3 p3, P4 p4) {
        Consumer<T> applySetter = instance -> setter.accept(instance, p1, p2, p3, p4);
        applySetters.add(applySetter);

        return this;
    }

    public <P1, P2, P3, P4, P5> Builder<T> with(Consumer5<T, P1, P2, P3, P4, P5> setter, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
        Consumer<T> applySetter = instance -> setter.accept(instance, p1, p2, p3, p4, p5);
        applySetters.add(applySetter);

        return this;
    }

    public T build() {
        T value = instantiator.get();
        applySetters.forEach(modifier -> modifier.accept(value));
        applySetters.clear();

        return value;
    }


    @FunctionalInterface
    public interface Consumer2<T, P1, P2> {
        void accept(T t, P1 p1, P2 p2);
    }

    @FunctionalInterface
    public interface Consumer3<T, P1, P2, P3> {
        void accept(T t, P1 p1, P2 p2, P3 p3);
    }

    @FunctionalInterface
    public interface Consumer4<T, P1, P2, P3, P4> {
        void accept(T t, P1 p1, P2 p2, P3 p3, P4 p4);
    }

    @FunctionalInterface
    public interface Consumer5<T, P1, P2, P3, P4, P5> {
        void accept(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }
}
