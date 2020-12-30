package com.infilos.utils;

import com.infilos.utils.pool.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author zhiguang.zhang on 2020-03-26.
 */

public abstract class Pool<A> {

    protected AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Returns the reference type of the objects stored in the pool.
     */
    public abstract PoolRefType refType();

    /**
     * Factory to create new objects.
     */
    protected abstract A create();

    /**
     * Resets the object's internal state, invoked when object added or release back to pool.
     */
    protected abstract void reset(A value);

    /**
     * Destructor to destroy objects, invoked when object evicted from the pool.
     */
    protected abstract void dispose(A value);

    /**
     * Method to check objects health, invoked when object leased from pool.
     */
    protected abstract boolean check(A value);

    /**
     * Try to acquire a lease for an object without blocking.
     */
    public Optional<PoolLease<A>> tryAcquire() {
        if (!closed.get()) {
            return handleTryAcquire();
        } else {
            throw PoolClosedException.create();
        }
    }

    protected abstract Optional<PoolLease<A>> handleTryAcquire();

    /**
     * Try to acquire a lease for an object blocking at most until the given duration.
     */
    public Optional<PoolLease<A>> tryAcquire(Duration atMost) {
        if (!closed.get()) {
            return handleTryAcquire(atMost);
        } else {
            throw PoolClosedException.create();
        }
    }

    protected abstract Optional<PoolLease<A>> handleTryAcquire(Duration atMost);

    /**
     * Acquire a lease for an object blocking if none is available.
     */
    public PoolLease<A> acquire() {
        if (!closed.get()) {
            return handleAcquire();
        } else {
            throw PoolClosedException.create();
        }
    }

    protected abstract PoolLease<A> handleAcquire();

    /**
     * Clear the object pool, i.e. evicts every object currently pooled.
     */
    public void clear() {
        if (!closed.get()) {
            handleClear();
        } else {
            throw PoolClosedException.create();
        }
    }

    protected abstract void handleClear();

    /**
     * Fills the object pool by creating (and pooling) new objects until the number of live objects
     * reaches the pool capacity.
     */
    public void fill() {
        if (!closed.get()) {
            handleFill();
        } else {
            throw PoolClosedException.create();
        }
    }

    protected abstract void handleFill();

    /**
     * Closes this pool, and properly disposes of each pooled object.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            handleClear();
            handleClose();
        }
    }

    protected abstract void handleClose();

    /**
     * Returns the capacity of the pool.
     */
    public abstract int capacity();

    /**
     * Returns the number of objects keeped in the pool.
     */
    public abstract int size();

    /**
     * Returns the number of live objects, in pool and leased.
     */
    public abstract int live();

    /**
     * Returns the number of leased objects.
     */
    public int leased() {
        return live() - size();
    }


    private static final Duration Infinite = Duration.ofSeconds(Long.MAX_VALUE);

    public static <A> Pool<A> fixedConsts(List<A> objects) {
        if(objects.isEmpty()) {
            throw new IllegalArgumentException("Pool elements must not be empty.");
        }
        if(objects.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Pool elements must not be null.");
        }

        int capacity = objects.size();
        Iterator<A> iterator = Collections.unmodifiableList(objects).iterator();

        return new PoolBuilder<A>().capacity(capacity).creator(iterator::next).build();
    }

    public static <A> PoolBuilder<A> builder() {
        return new PoolBuilder<>();
    }

    public static final class PoolBuilder<A> {
        private int capacity;
        private Supplier<A> creator;
        private Consumer<A> reseter = a -> {
        };
        private Consumer<A> disposer = a -> {
        };
        private Function<A, Boolean> checker = a -> true;
        private PoolRefType refType = PoolRefType.Strong;
        private Duration maxIdleTime = Infinite;

        PoolBuilder() {
        }

        public PoolBuilder<A> capacity(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Pool capacity must >= 0.");
            }
            this.capacity = capacity;
            return this;
        }

        public PoolBuilder<A> creator(Supplier<A> creator) {
            if (creator==null) {
                throw new IllegalArgumentException("Pool creator must not be null.");
            }
            this.creator = creator;
            return this;
        }

        public PoolBuilder<A> reseter(Consumer<A> reseter) {
            if (reseter==null) {
                throw new IllegalArgumentException("Pool reseter must not be null.");
            }
            this.reseter = reseter;
            return this;
        }

        public PoolBuilder<A> disposer(Consumer<A> disposer) {
            if (disposer==null) {
                throw new IllegalArgumentException("Pool disposer must not be null.");
            }
            this.disposer = disposer;
            return this;
        }

        public PoolBuilder<A> checker(Function<A, Boolean> checker) {
            if (checker==null) {
                throw new IllegalArgumentException("Pool checker must not be null.");
            }
            this.checker = checker;
            return this;
        }

        public PoolBuilder<A> refType(PoolRefType refType) {
            this.refType = refType;
            return this;
        }

        public PoolBuilder<A> maxIdleTime(Duration maxIdleTime) {
            if (maxIdleTime==null) {
                throw new IllegalArgumentException("Pool max-idle-time must not be null.");
            }
            this.maxIdleTime = maxIdleTime;
            return this;
        }

        public Pool<A> build() {
            if (maxIdleTime.equals(Infinite)) {
                return new SimplePool<>(capacity, refType, creator, reseter, disposer, checker);
            } else {
                return new ExpiringPool<>(capacity, refType, maxIdleTime, creator, reseter, disposer, checker);
            }
        }
    }
}
