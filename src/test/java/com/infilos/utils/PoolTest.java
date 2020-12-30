package com.infilos.utils;

import com.infilos.utils.pool.*;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public class PoolTest {

    @Test
    public void test() {
        Pool<String> pool = Pool.<String>builder().capacity(1).build();
        Pool.PoolBuilder<String> builder = Pool.<String>builder().capacity(1);
        Pool<String> fixed = Pool.fixedConsts(Arrays.asList("a", "b"));

        assertTrue(Pool.builder().capacity(1).creator(Object::new).build() instanceof SimplePool);
        assertTrue(Pool.builder().capacity(1).creator(Object::new).maxIdleTime(Duration.ofSeconds(1)).build() instanceof ExpiringPool);
        assertSame(Pool.builder().capacity(1).creator(Object::new).build().refType(), PoolRefType.Strong);
        assertSame(Pool.builder().capacity(1).creator(Object::new).refType(PoolRefType.Weak).build().refType(), PoolRefType.Weak);
        assertSame(Pool.builder().capacity(1).creator(Object::new).refType(PoolRefType.Soft).build().refType(), PoolRefType.Soft);
    }

    @Test
    public void testCapacity() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        assertEquals(3, pool.capacity());
    }

    @Test
    public void testLiveAndLeased() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        pool.acquire();

        assertEquals(0, pool.size());
        assertEquals(1, pool.leased());
        assertEquals(1, pool.live());
    }

    @Test
    public void testLazyCreator() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Object> pool = Pool.builder().capacity(3).creator(new Supplier<Object>() {
            @Override
            public Object get() {
                counter.incrementAndGet();
                return new Object();
            }
        }).build();

        assertEquals(0, pool.size());
        assertEquals(0, pool.live());

        pool.acquire();

        assertEquals(0, pool.size());
        assertEquals(1, pool.live());
        assertEquals(1, counter.get());

        pool.acquire();

        assertEquals(0, pool.size());
        assertEquals(2, pool.live());
        assertEquals(2, counter.get());
    }

    @Test
    public void testLazyCreatorIfExists() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Object> pool = Pool.builder().capacity(3).creator(new Supplier<Object>() {
            @Override
            public Object get() {
                counter.incrementAndGet();
                return new Object();
            }
        }).build();

        assertEquals(0, pool.size());
        assertEquals(0, pool.live());

        pool.acquire().release();

        assertEquals(1, pool.size());
        assertEquals(1, pool.live());
        assertEquals(1, counter.get());

        pool.acquire();

        assertEquals(0, pool.size());
        assertEquals(1, pool.live());
        assertEquals(1, counter.get());
    }

    @Test
    public void testFill() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();

        pool.fill();

        assertEquals(3, pool.size());
        assertEquals(3, pool.live());
    }

    @Test
    public void testClear() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();

        pool.fill();
        assertEquals(3, pool.size());

        pool.clear();
        assertEquals(0, pool.size());
        assertEquals(0, pool.live());
    }

    @Test
    public void testBlockWhenNonAvailable() throws ExecutionException, InterruptedException {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        pool.fill();

        pool.acquire();
        pool.acquire();
        PoolLease<Object> object = pool.acquire();
        System.out.println("non available...");

        CompletableFuture<Object> blockingAcquire = CompletableFuture.supplyAsync(new Supplier<Object>() {
            @Override
            public Object get() {
                Object acquired = pool.acquire().get();
                System.out.println("acquired...");
                return acquired;
            }
        });

        CompletableFuture.runAsync(() -> {
            Threads.sleep(1);
            object.release();
            System.out.println("released...");
        });

        blockingAcquire.join();
        assertNotNull(blockingAcquire.get());
    }

    @Test
    public void testAcquireWithDuration() throws ExecutionException, InterruptedException {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        pool.fill();

        pool.acquire();
        pool.acquire();
        pool.acquire();

        CompletableFuture<Optional<PoolLease<Object>>> blockingAcquire = CompletableFuture.supplyAsync(
            () -> pool.tryAcquire(Duration.ofSeconds(1))
        );

        blockingAcquire.join();
        assertFalse(blockingAcquire.get().isPresent());
    }

    @Test
    public void testResetWhenAddingAndReleasing() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Object> pool = Pool.builder()
            .capacity(3)
            .creator(Object::new)
            .reseter(obj -> counter.incrementAndGet())
            .build();

        pool.fill();

        assertEquals(3, pool.size());
        assertEquals(3, counter.get());
    }

    @Test
    public void testResetWhenReleasing() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Object> pool = Pool.builder()
            .capacity(3)
            .creator(Object::new)
            .reseter(obj -> counter.incrementAndGet())
            .build();

        pool.acquire().release();

        assertEquals(1, pool.size());
        assertEquals(1, counter.get());
    }

    @Test
    public void testDisposeAfterInvalidated() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Object> pool = Pool.builder()
            .capacity(3)
            .creator(Object::new)
            .disposer(obj -> counter.incrementAndGet())
            .build();

        pool.acquire().invalidate();

        assertEquals(1, counter.get());
    }

    @Test
    public void testDisposeAfterCleared() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Object> pool = Pool.builder()
            .capacity(3)
            .creator(Object::new)
            .disposer(obj -> counter.incrementAndGet())
            .build();

        pool.fill();
        pool.clear();

        assertEquals(3, counter.get());
    }

    @Test
    public void testDisposeAfterCheck() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean failOnce = new AtomicBoolean(false);

        Pool<Object> pool = Pool.builder()
            .capacity(3)
            .creator(Object::new)
            .disposer(obj -> counter.incrementAndGet())
            .checker(obj -> {
                if(!failOnce.get()) { // fail on check when first acquire
                    failOnce.getAndSet(true);
                    return false;
                } else {
                    return true;
                }
            })
            .build();

        pool.fill();
        pool.acquire();

        assertEquals(1, counter.get());
    }

    @Test
    public void tesGCBasedtevictionForWeak() {
        AtomicInteger counter = new AtomicInteger(0);
        Pool<Object> pool = Pool.builder()
            .capacity(3)
            .creator(() -> {
                counter.incrementAndGet();
                System.out.println("created...");
                return new Object();
            })
            .refType(PoolRefType.Weak) // soft can be collect only occur OOM
            .build();

        pool.fill();
        assertEquals(3, pool.size());
        assertEquals(3, counter.get());

        System.gc();

        pool.acquire();
        assertEquals(4, counter.get());
    }

    @Test
    public void testClearAfterClosed() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();

        pool.fill();
        assertEquals(3, pool.live());

        pool.close();
        assertEquals(0, pool.live());
    }

    @Test(expected = PoolClosedException.class)
    public void testThrownAfterAcquireFromClosedPool() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        pool.close();

        pool.acquire();
    }

    @Test
    public void testInvalidatedFromPool() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        pool.fill();

        assertEquals(3, pool.live());
        assertEquals(3, pool.size());

        PoolLease<Object> lease = pool.acquire();
        lease.invalidate();

        assertEquals(2, pool.live());
        assertEquals(2, pool.size());
    }

    @Test
    public void testReleaseBackToPool() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        pool.fill();

        assertEquals(3, pool.live());
        assertEquals(3, pool.size());

        PoolLease<Object> lease = pool.acquire();
        lease.release();

        assertEquals(3, pool.live());
        assertEquals(3, pool.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testThrownOnInvalidatedLease() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        pool.fill();

        assertEquals(3, pool.live());
        assertEquals(3, pool.size());

        PoolLease<Object> lease = pool.acquire();
        lease.invalidate();

        lease.get();
    }

    @Test(expected = IllegalStateException.class)
    public void testThrownOnReleasedLease() {
        Pool<Object> pool = Pool.builder().capacity(3).creator(Object::new).build();
        pool.fill();

        assertEquals(3, pool.live());
        assertEquals(3, pool.size());

        PoolLease<Object> lease = pool.acquire();
        lease.release();

        lease.get();
    }

    @Test(expected = IllegalStateException.class)
    public void testThrownOnUseAndReleaseLease() {
        Pool<Object> pool = Pool.builder().capacity(1).creator(Object::new).build();
        pool.fill();

        PoolLease<Object> lease = pool.acquire();
        assertEquals(0, pool.size());

        lease.map(obj -> ""); // released

        lease.get();
    }

    @Test
    public void testUseAndInvalidateLease() {
        Pool<Object> pool = Pool.builder().capacity(1).creator(Object::new).build();
        pool.fill();

        PoolLease<Object> lease = pool.acquire();
        assertEquals(0, pool.size());

        lease.map(obj -> {
            lease.invalidate();
            return "";
        }); // released

        assertEquals(0, pool.size());
    }

    @Test
    public void testReleaseToClosedPoolWillBeDestroy() {
        Pool<Object> pool = Pool.builder().capacity(1).creator(Object::new).build();
        PoolLease<Object> lease = pool.acquire();

        pool.close();
        assertEquals(1, pool.live());

        lease.release();

        assertEquals(0, pool.live());
        assertEquals(0, pool.size());
    }

    @Test
    public void testEvictIdleObjects() {
        Pool<Object> pool = Pool.builder()
            .capacity(3)
            .creator(Object::new)
            .refType(PoolRefType.Strong)
            .maxIdleTime(Duration.ofMillis(100))
            .build();

        pool.fill();

        assertEquals(3, pool.live());
        assertEquals(3, pool.size());

        Threads.sleep(1);

        assertEquals(0, pool.live());
        assertEquals(0, pool.size());
    }

    @Test
    public void testEvictAndReleaseObjects() {
        Pool<Object> pool = Pool.builder()
            .capacity(3)
            .creator(Object::new)
            .refType(PoolRefType.Strong)
            .maxIdleTime(Duration.ofMillis(100))
            .build();

        pool.fill();

        PoolLease<Object> lease = pool.acquire();

        assertEquals(2, pool.size());
        assertEquals(3, pool.live());

        Threads.sleep(1);

        assertEquals(0, pool.size());
        assertEquals(1, pool.live());

        lease.release();

        assertEquals(1, pool.size());
        assertEquals(1, pool.live());

        Threads.sleep(1);

        assertEquals(0, pool.size());
        assertEquals(0, pool.live());
    }
}