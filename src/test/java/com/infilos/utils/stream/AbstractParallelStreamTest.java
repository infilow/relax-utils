package com.infilos.utils.stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.BaseStream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"ResultOfMethodCallIgnored", "TestMethodWithIncorrectSignature"})
public abstract class AbstractParallelStreamTest<T, S extends BaseStream<T, S>, R extends AbstractParallelStream<T, S>> {

    ForkJoinPool workerPool;
    S delegateMock;
    R parallelStreamSupportMock;

    protected abstract R createParallelStreamSupportMock(ForkJoinPool workerPool);

    @Before
    public void before() {
        this.workerPool = new ForkJoinPool(1);
        this.parallelStreamSupportMock = createParallelStreamSupportMock(this.workerPool);
        this.delegateMock = this.parallelStreamSupportMock.delegate;
    }

    @After
    public void after() throws InterruptedException {
        this.workerPool.shutdown();
        this.workerPool.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void iterator() {
        Iterator<?> iteratorMock = mock(Iterator.class);
        when(this.delegateMock.iterator()).thenReturn((Iterator) iteratorMock);
        Iterator<?> iterator = this.parallelStreamSupportMock.iterator();

        verify(this.delegateMock).iterator();
        assertSame(iteratorMock, iterator);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void spliterator() {
        Spliterator<?> spliteratorMock = mock(Spliterator.class);
        when(this.delegateMock.spliterator()).thenReturn((Spliterator) spliteratorMock);
        Spliterator<?> spliterator = this.parallelStreamSupportMock.spliterator();

        verify(this.delegateMock).spliterator();
        assertSame(spliteratorMock, spliterator);
    }

    @Test
    public void isParallel() {
        when(this.delegateMock.isParallel()).thenReturn(true);
        boolean parallel = this.parallelStreamSupportMock.isParallel();

        verify(this.delegateMock).isParallel();
        assertTrue(parallel);
    }

    @Test
    public void sequential() {
        BaseStream<?, ?> stream = this.parallelStreamSupportMock.sequential();

        verify(this.delegateMock).sequential();
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void parallel() {
        BaseStream<?, ?> stream = this.parallelStreamSupportMock.parallel();

        verify(this.delegateMock).parallel();
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void unordered() {
        BaseStream<?, ?> stream = this.parallelStreamSupportMock.unordered();

        verify(this.delegateMock).unordered();
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void onClose() {
        Runnable r = () -> {
        };
        BaseStream<?, ?> stream = this.parallelStreamSupportMock.onClose(r);

        verify(this.delegateMock).onClose(r);
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void close() {
        this.parallelStreamSupportMock.close();

        verify(this.delegateMock).close();
    }

    @Test
    public void executeWithRunnable() {
        AtomicBoolean b = new AtomicBoolean(false);

        this.parallelStreamSupportMock.execute(() -> b.set(true));

        assertTrue(b.get());
    }

    @Test
    public void executeWithRunnableThrowingException() {
        Runnable r = () -> {
            throw new RuntimeException("boom");
        };

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> this.parallelStreamSupportMock.execute(r));
    }

    @Test
    public void executeWithCallable() {
        AtomicBoolean b = new AtomicBoolean(false);
        Callable<Void> c = () -> {
            b.set(true);
            return null;
        };

        this.parallelStreamSupportMock.execute(c);

        assertTrue(b.get());
    }

    @Test
    public void executeWithCallableThrowingError() {
        Callable<Void> c = () -> {
            throw new AssertionError("boom");
        };

        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> this.parallelStreamSupportMock.execute(c));
    }

    @Test
    public void executeWithCallableThrowingCheckedException() {
        Exception e = new Exception("boom");
        try {
            Callable<Void> c = () -> {
                throw e;
            };

            this.parallelStreamSupportMock.execute(c);
            fail("Expect runtime exception.");
        } catch (RuntimeException rte) {
            assertEquals(e, rte.getCause());
        }
    }
}