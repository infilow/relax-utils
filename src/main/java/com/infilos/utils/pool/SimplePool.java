package com.infilos.utils.pool;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author zhiguang.zhang on 2020-03-27.
 */

public class SimplePool<A> extends AbstractPool<A> {
    private final Supplier<A> creator;
    private final Consumer<A> reseter;
    private final Consumer<A> disposer;
    private final Function<A, Boolean> checker;

    public SimplePool(int capacity,
               PoolRefType refType,
               Supplier<A> creator,
               Consumer<A> reseter,
               Consumer<A> disposer,
               Function<A, Boolean> checker) {
        super(capacity, refType);
        this.creator = creator;
        this.reseter = reseter;
        this.disposer = disposer;
        this.checker = checker;
    }

    @Override
    protected PoolItem createItem(A value) {
        return new SimpleItem(PoolRef.create(value, refType()));
    }

    @Override
    protected void handleClose() {
    }

    @Override
    protected A create() {
        return creator.get();
    }

    @Override
    protected void reset(A value) {
        reseter.accept(value);
    }

    @Override
    protected void dispose(A value) {
        disposer.accept(value);
    }

    @Override
    protected boolean check(A value) {
        return checker.apply(value);
    }

    protected final class SimpleItem extends PoolItem {

        SimpleItem(PoolRef<A> ref) {
            super(ref);
        }

        @Override
        public void offerSuccess() {
        }

        @Override
        public void consume() {
        }
    }
}
