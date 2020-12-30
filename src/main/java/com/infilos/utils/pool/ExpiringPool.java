package com.infilos.utils.pool;

import java.time.Duration;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author zhiguang.zhang on 2020-03-27.
 */

public class ExpiringPool<A> extends AbstractPool<A> {
    private final Duration maxIdleTime;
    private final Supplier<A> creator;
    private final Consumer<A> reseter;
    private final Consumer<A> disposer;
    private final Function<A, Boolean> checker;

    private static final AtomicInteger TimerThreadCounter = new AtomicInteger(0);
    private final Timer timer = new Timer("Pool-Expire-Thread-" + TimerThreadCounter.getAndIncrement(), true);
    private final PoolAdder adder = PoolAdder.create();

    public ExpiringPool(int capacity,
                        PoolRefType refType,
                        Duration maxIdleTime,
                        Supplier<A> creator,
                        Consumer<A> reseter,
                        Consumer<A> disposer,
                        Function<A, Boolean> checker) {
        super(capacity, refType);
        this.maxIdleTime = maxIdleTime;
        this.creator = creator;
        this.reseter = reseter;
        this.disposer = disposer;
        this.checker = checker;
    }

    @Override
    protected PoolItem createItem(A value) {
        adder.increment();
        long id = adder.count();
        PoolRef<A> ref = PoolRef.create(value, refType());

        return new ExpiringItem(id, ref, new TimerTask() {
            @Override
            public void run() {
                ExpiringItem item = new ExpiringItem(id, ref);
                if (items.remove(item)) {
                    item.destroy();
                }
            }
        });
    }

    @Override
    protected void handleClose() {
        timer.cancel();
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

    protected final class ExpiringItem extends PoolItem {
        private long id;
        private TimerTask task;

        ExpiringItem(long id, PoolRef<A> ref, TimerTask task) {
            super(ref);
            this.id = id;
            this.task = task;
        }

        ExpiringItem(long id, PoolRef<A> ref) {
            super(ref);
            this.id = id;
            this.task = new TimerTask() {
                @Override
                public void run() {
                }
            };
        }

        @Override
        public void offerSuccess() {
            try {
                timer.schedule(task, maxIdleTime.toMillis());
            } catch (IllegalStateException ignore) {
            }
        }

        @Override
        public void consume() {
            task.cancel();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }

        @Override
        public boolean equals(Object that) {
            if (Objects.isNull(that) || !Objects.equals(this.getClass(), that.getClass())) {
                return false;
            }

            return this.hashCode()==that.hashCode();
        }
    }
}
