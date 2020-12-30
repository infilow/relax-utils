package com.infilos.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author zhiguang.zhang on 2020-03-26.
 */

public abstract class PoolLease<A> {

    private final AtomicBoolean dirty = new AtomicBoolean(false);

    /**
     * Returns the object leased by the pool.
     */
    public A get() {
        if(!dirty.get()) {
            return value();
        } else {
            throw new IllegalStateException("Tried to get an object from an already released or invalidated lease.");
        }
    }

    protected abstract A value();

    /**
     * Releases the object back to the pool for reuse.
     */
    public void release() {
        if(dirty.compareAndSet(false, true)) {
            handleRelease();
        }
    }

    protected abstract void handleRelease();

    /**
     * Invalidates the current lease, will destroy the object and not return to pool.
     */
    public void invalidate() {
        if(dirty.compareAndSet(false, true)) {
            handleInvalidate();
        }
    }

    protected abstract void handleInvalidate();

    /**
     * Gets the value from the lease, passing it onto the provided function and
     * releasing it back to the pool after the function complete.
     */
    public <B> B map(Function<A,B> function) {
        try {
            return function.apply(get());
        } finally {
            release();
        }
    }
}
