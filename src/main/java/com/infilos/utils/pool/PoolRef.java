package com.infilos.utils.pool;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * @author zhiguang.zhang on 2020-03-26.
 */

public abstract class PoolRef<A> {

    public abstract Optional<A> toOption();

    static class StrongRef<A> extends PoolRef<A> {
        private final A value;

        StrongRef(A value) {
            this.value = value;
        }

        @Override
        public Optional<A> toOption() {
            return Optional.of(value);
        }
    }

    static class SoftRef<A> extends PoolRef<A> {
        private final SoftReference<A> value;

        SoftRef(SoftReference<A> value) {
            this.value = value;
        }

        @Override
        public Optional<A> toOption() {
            return Optional.ofNullable(value.get());
        }
    }

    static class WeakRef<A> extends PoolRef<A> {
        private final WeakReference<A> value;

        WeakRef(WeakReference<A> value) {
            this.value = value;
        }

        @Override
        public Optional<A> toOption() {
            return Optional.ofNullable(value.get());
        }
    }

    public static <A> PoolRef<A> create(A value, PoolRefType refType) {
        PoolRef<A> ref;
        switch (refType) {
            case Strong:
                ref = new StrongRef<>(value);
                break;
            case Soft:
                ref = new SoftRef<>(new SoftReference<>(value));
                break;
            case Weak:
                ref = new WeakRef<>(new WeakReference<>(value));
                break;
            default:
                throw new IllegalArgumentException();
        }

        return ref;
    }
}
