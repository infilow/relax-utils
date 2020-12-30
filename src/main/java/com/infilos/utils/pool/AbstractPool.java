package com.infilos.utils.pool;

import com.infilos.utils.Pool;
import com.infilos.utils.PoolLease;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhiguang.zhang on 2020-03-27.
 */

public abstract class AbstractPool<A> extends Pool<A> {
    private final int capacity;
    private final PoolRefType refType;

    protected ArrayBlockingQueue<PoolItem> items;
    private final AtomicInteger live = new AtomicInteger(0);

    AbstractPool(int capacity, PoolRefType refType) {
        this.capacity = capacity;
        this.refType = refType;
        this.items = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public PoolRefType refType() {
        return refType;
    }

    protected void destory(A value) {
        dispose(value);
        decrementLive();
    }

    private void decrementLive() {
        live.getAndDecrement();
    }

    protected abstract PoolItem createItem(A value);

    private void tryOffer(A value) {
        PoolItem item = createItem(value);
        if (items.offer(item)) {
            item.offerSuccess();
        } else {
            destory(value);
        }
    }

    private Optional<A> tryCreate() {
        if (live.getAndIncrement() < capacity) {
            return Optional.of(create());
        } else {
            decrementLive();
            return Optional.empty();
        }
    }

    private Optional<A> unwrapItem(PoolItem nullableWrappedItem, boolean retry) {
        Optional<PoolItem> wrapped = Optional.ofNullable(nullableWrappedItem);

        if (wrapped.isPresent() && wrapped.get().isDefined()) {
            return Optional.of(wrapped.get().get());
        } else if (wrapped.isPresent()) {
            wrapped.get().destroy();
            if (retry) {
                // todo
                //return unwrapItem(nullableWrappedItem, true);
                return Optional.empty();
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    protected PoolLease<A> handleAcquire() {
        Optional<A> item = unwrapItem(items.poll(), true);

        if (item.isPresent()) {
            return new PredefPoolLease(item.get());
        } else {
            try {
                Optional<A> created = tryCreate();
                if (created.isPresent()) {
                    return new PredefPoolLease(created.get());
                } else {
                    return new PredefPoolLease(unwrapItem(items.take(), true).get());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e); // interrupted during waiting for available item
            }
        }
    }

    protected Optional<PoolLease<A>> handleTryAcquire() {
        Optional<A> item = unwrapItem(items.poll(), true);

        if (item.isPresent()) {
            return Optional.of(new PredefPoolLease(item.get()));
        } else {
            return tryCreate().map(PredefPoolLease::new);
        }
    }

    protected void handleClear() {
        Optional<PoolItem> itemOption = Optional.ofNullable(items.poll());
        if (itemOption.isPresent()) {
            itemOption.get().destroy();
            handleClear();
        }
    }

    protected void handleFill() {
        Optional<A> valueOpt = tryCreate();
        if (valueOpt.isPresent()) {
            A value = valueOpt.get();
            reset(value);
            tryOffer(value);
            handleFill();
        }
    }

    public int capacity() {
        return capacity;
    }

    public int size() {
        return items.size();
    }

    public int live() {
        return live.get();
    }

    protected Optional<PoolLease<A>> handleTryAcquire(Duration atMost) {
        Optional<A> item = unwrapItem(items.poll(), true);

        if (item.isPresent()) {
            return Optional.of(new PredefPoolLease(item.get()));
        } else {
            try {
                return unwrapItem(items.poll(atMost.toNanos(), TimeUnit.NANOSECONDS), false).map(PredefPoolLease::new);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);  // interrupted during waiting for available item
            }
        }
    }


    private class PredefPoolLease extends PoolLease<A> {
        protected A value;

        PredefPoolLease(A value) {
            this.value = value;
        }

        @Override
        protected A value() {
            return value;
        }

        @Override
        protected void handleRelease() {
            if (!closed.get()) {
                reset(value);
                tryOffer(value);
            } else {
                destory(value);
            }
        }

        @Override
        protected void handleInvalidate() {
            destory(value);
        }
    }


    protected abstract class PoolItem {
        private PoolRef<A> ref;

        PoolItem(PoolRef<A> ref) {
            this.ref = ref;
        }

        boolean isDefined() {
            Optional<A> refOption = ref.toOption();
            return refOption.isPresent() && check(refOption.get());
        }

        A get() {
            A value = ref.toOption().orElse(null);
            consume();
            return value;
        }

        public void destroy() {
            Optional<A> refOption = ref.toOption();
            if(refOption.isPresent()) {
                dispose(refOption.get());
            }
            decrementLive();
            consume();
        }

        public abstract void offerSuccess();

        public abstract void consume();
    }
}
