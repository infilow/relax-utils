package com.infilos.utils.stream;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.IntStream.Builder;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.lang.Thread.currentThread;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"ResultOfMethodCallIgnored", "rawtypes"})
public class ParallelIntStreamTest extends AbstractParallelStreamTest<Integer, IntStream, ParallelIntStream> {

    private Stream<?> mappedDelegateMock;
    private IntStream mappedIntDelegateMock;
    private LongStream mappedLongDelegateMock;
    private DoubleStream mappedDoubleDelegateMock;
    private PrimitiveIterator.OfInt iteratorMock;
    private Spliterator.OfInt spliteratorMock;
    private int[] toArrayResult;
    private IntSummaryStatistics summaryStatistics;

    private ParallelIntStream parallelIntStreamSupport;

    @Override
    protected ParallelIntStream createParallelStreamSupportMock(ForkJoinPool workerPool) {
        return new ParallelIntStream(mock(IntStream.class), workerPool);
    }

    @Before
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void init() {
        // Precondition for all tests
        assertFalse("This test must not run in a ForkJoinPool", currentThread() instanceof ForkJoinWorkerThread);

        this.mappedDelegateMock = mock(Stream.class);
        this.mappedIntDelegateMock = mock(IntStream.class);
        this.mappedLongDelegateMock = mock(LongStream.class);
        this.mappedDoubleDelegateMock = mock(DoubleStream.class);
        this.iteratorMock = mock(PrimitiveIterator.OfInt.class);
        this.spliteratorMock = mock(Spliterator.OfInt.class);
        this.toArrayResult = new int[0];
        this.summaryStatistics = new IntSummaryStatistics();

        when(this.delegateMock.map(any())).thenReturn(this.mappedIntDelegateMock);
        when(this.delegateMock.mapToObj(any())).thenReturn((Stream) this.mappedDelegateMock);
        when(this.delegateMock.mapToLong(any())).thenReturn(this.mappedLongDelegateMock);
        when(this.delegateMock.mapToDouble(any())).thenReturn(this.mappedDoubleDelegateMock);
        when(this.delegateMock.flatMap(any())).thenReturn(this.mappedIntDelegateMock);
        when(this.delegateMock.iterator()).thenReturn(this.iteratorMock);
        when(this.delegateMock.spliterator()).thenReturn(this.spliteratorMock);
        when(this.delegateMock.isParallel()).thenReturn(false);
        when(this.delegateMock.toArray()).thenReturn(this.toArrayResult);
        when(this.delegateMock.reduce(anyInt(), any())).thenReturn(42);
        when(this.delegateMock.reduce(any())).thenReturn(OptionalInt.of(42));
        when(this.delegateMock.collect(any(), any(), any())).thenReturn("collect");
        when(this.delegateMock.sum()).thenReturn(42);
        when(this.delegateMock.min()).thenReturn(OptionalInt.of(42));
        when(this.delegateMock.max()).thenReturn(OptionalInt.of(42));
        when(this.delegateMock.count()).thenReturn(42L);
        when(this.delegateMock.average()).thenReturn(OptionalDouble.of(42.0));
        when(this.delegateMock.summaryStatistics()).thenReturn(this.summaryStatistics);
        when(this.delegateMock.anyMatch(any())).thenReturn(true);
        when(this.delegateMock.allMatch(any())).thenReturn(true);
        when(this.delegateMock.noneMatch(any())).thenReturn(true);
        when(this.delegateMock.findFirst()).thenReturn(OptionalInt.of(42));
        when(this.delegateMock.findAny()).thenReturn(OptionalInt.of(42));
        when(this.delegateMock.asLongStream()).thenReturn(this.mappedLongDelegateMock);
        when(this.delegateMock.asDoubleStream()).thenReturn(this.mappedDoubleDelegateMock);
        when(this.delegateMock.boxed()).thenReturn((Stream) this.mappedDelegateMock);

        IntStream delegate = IntStream.of(1).parallel();
        this.parallelIntStreamSupport = new ParallelIntStream(delegate, this.workerPool);
    }

    @Test
    public void parallelStreamWithArray() {
        IntStream stream = ParallelIntStream.submit(new int[]{42}, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertEquals(OptionalInt.of(42), stream.findAny());
    }

    @Test
    public void parallelStreamWithNullArray() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> ParallelIntStream.submit((int[]) null, this.workerPool));
    }

    @Test
    public void parallelStreamSupportWithSpliterator() {
        Spliterator.OfInt spliterator = IntStream.of(42).spliterator();
        IntStream stream = ParallelIntStream.submit(spliterator, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertEquals(OptionalInt.of(42), stream.findAny());
    }

    @Test
    public void parallelStreamSupportWithNullSpliterator() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> ParallelIntStream.submit((Spliterator.OfInt) null, this.workerPool));
    }

    @Test
    public void parallelStreamSupportWithSpliteratorSupplier() {
        Supplier<Spliterator.OfInt> supplier = () -> IntStream.of(42).spliterator();
        IntStream stream = ParallelIntStream.submit(supplier, 0, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertEquals(OptionalInt.of(42), stream.findAny());
    }

    @Test
    public void parallelStreamSupportWithNullSpliteratorSupplier() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> ParallelIntStream.submit(null, 0, this.workerPool));
    }

    @Test
    public void parallelStreamWithBuilder() {
        Builder builder = IntStream.builder();
        builder.accept(42);
        IntStream stream = ParallelIntStream.submit(builder, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertEquals(OptionalInt.of(42), stream.findAny());
    }

    @Test
    public void parallelStreamWithNullBuilder() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> ParallelIntStream.submit((Builder) null, this.workerPool));
    }

    @Test
    public void iterate() {
        IntUnaryOperator operator = a -> a;
        IntStream stream = ParallelIntStream.iterate(42, operator, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertEquals(OptionalInt.of(42), stream.findAny());
    }

    @Test
    public void iterateWithNullOperator() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> ParallelIntStream.iterate(42, null, this.workerPool));
    }

    @Test
    public void generate() {
        IntSupplier supplier = () -> 42;
        IntStream stream = ParallelIntStream.generate(supplier, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertEquals(OptionalInt.of(42), stream.findAny());
    }

    @Test
    public void generateWithNullSupplier() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> ParallelIntStream.generate(null, this.workerPool));
    }

    @Test
    public void range() {
        IntStream stream = ParallelIntStream.range(0, 5, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertArrayEquals(stream.toArray(), new int[]{0, 1, 2, 3, 4});
    }

    @Test
    public void rangeClosed() {
        IntStream stream = ParallelIntStream.rangeClosed(0, 5, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertArrayEquals(stream.toArray(), new int[]{0, 1, 2, 3, 4, 5});
    }

    @Test
    public void concat() {
        IntStream a = IntStream.of(42);
        IntStream b = IntStream.of(43);
        IntStream stream = ParallelIntStream.concat(a, b, this.workerPool);

        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertTrue(stream.isParallel());
        assertArrayEquals(stream.toArray(), new int[]{42, 43});
    }

    @Test
    public void concatWithNullStreamA() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> ParallelIntStream.concat(null, IntStream.of(42), this.workerPool));
    }

    @Test
    public void concatWithNullStreamB() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> ParallelIntStream.concat(IntStream.of(42), null, this.workerPool));
    }

    @Test
    public void filter() {
        IntPredicate p = i -> true;
        IntStream stream = this.parallelStreamSupportMock.filter(p);

        verify(this.delegateMock).filter(p);
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void map() {
        IntUnaryOperator f = i -> 42;
        IntStream stream = this.parallelStreamSupportMock.map(f);

        verify(this.delegateMock).map(f);
        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertSame(((ParallelIntStream) stream).delegate, this.mappedIntDelegateMock);
        assertSame(((ParallelIntStream) stream).workerPool, this.workerPool);
    }

    @Test
    public void mapToObj() {
        IntFunction<String> f = i -> "x";
        Stream<String> stream = this.parallelStreamSupportMock.mapToObj(f);

        verify(this.delegateMock).mapToObj(f);
        assertThat(stream, instanceOf(ParallelStream.class));
        assertSame(((ParallelStream) stream).delegate, this.mappedDelegateMock);
        assertSame(((ParallelStream) stream).workerPool, this.workerPool);
    }

    @Test
    public void mapToLong() {
        IntToLongFunction f = i -> 1L;
        LongStream stream = this.parallelStreamSupportMock.mapToLong(f);

        verify(this.delegateMock).mapToLong(f);
        assertThat(stream, instanceOf(ParallelLongStream.class));
        assertSame(((ParallelLongStream) stream).delegate, this.mappedLongDelegateMock);
        assertSame(((ParallelLongStream) stream).workerPool, this.workerPool);
    }

    @Test
    public void mapToDouble() {
        IntToDoubleFunction f = i -> 1.0;
        DoubleStream stream = this.parallelStreamSupportMock.mapToDouble(f);

        verify(this.delegateMock).mapToDouble(f);
        assertThat(stream, instanceOf(ParallelDoubleStream.class));
        assertSame(((ParallelDoubleStream) stream).delegate, this.mappedDoubleDelegateMock);
        assertSame(((ParallelDoubleStream) stream).workerPool, this.workerPool);
    }

    @Test
    public void flatMap() {
        IntFunction<IntStream> f = i -> IntStream.of(1);
        IntStream stream = this.parallelStreamSupportMock.flatMap(f);

        verify(this.delegateMock).flatMap(f);
        assertThat(stream, instanceOf(ParallelIntStream.class));
        assertSame(((ParallelIntStream) stream).delegate, this.mappedIntDelegateMock);
    }

    @Test
    public void distinct() {
        IntStream stream = this.parallelStreamSupportMock.distinct();

        verify(this.delegateMock).distinct();
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void sorted() {
        IntStream stream = this.parallelStreamSupportMock.sorted();

        verify(this.delegateMock).sorted();
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void peek() {
        IntConsumer c = i -> {
        };
        IntStream stream = this.parallelStreamSupportMock.peek(c);

        verify(this.delegateMock).peek(c);
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void limit() {
        IntStream stream = this.parallelStreamSupportMock.limit(5);

        verify(this.delegateMock).limit(5);
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void skip() {
        IntStream stream = this.parallelStreamSupportMock.skip(5);

        verify(this.delegateMock).skip(5);
        assertSame(this.parallelStreamSupportMock, stream);
    }

    @Test
    public void forEach() {
        IntConsumer c = i -> {
        };
        this.parallelStreamSupportMock.forEach(c);

        verify(this.delegateMock).forEach(c);
    }

    @Test
    public void forEachSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport.forEach(i -> threadRef.set(currentThread()));

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void forEachParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport.forEach(i -> threadRef.set(currentThread()));

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void forEachOrdered() {
        IntConsumer c = i -> {
        };
        this.parallelStreamSupportMock.forEachOrdered(c);

        verify(this.delegateMock).forEachOrdered(c);
    }

    @Test
    public void forEachOrderedSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport.forEachOrdered(i -> threadRef.set(currentThread()));

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void forEachOrderedParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport.forEachOrdered(i -> threadRef.set(currentThread()));

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void toArray() {
        int[] array = this.parallelStreamSupportMock.toArray();

        verify(this.delegateMock).toArray();
        assertSame(this.toArrayResult, array);
    }

    @Test
    public void toArraySequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .toArray();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void toArrayParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .toArray();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void reduceWithIdentityAndAccumulator() {
        IntBinaryOperator accumulator = (a, b) -> b;
        int result = this.parallelStreamSupportMock.reduce(0, accumulator);

        verify(this.delegateMock).reduce(0, accumulator);
        assertEquals(42, result);
    }

    @Test
    public void reduceWithIdentityAndAccumulatorSequential() {
        this.parallelIntStreamSupport.sequential();
        IntBinaryOperator accumulator = (a, b) -> b;
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .reduce(0, accumulator);

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void reduceWithIdentityAndAccumulatorParallel() {
        this.parallelIntStreamSupport.parallel();
        IntBinaryOperator accumulator = (a, b) -> b;
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .reduce(0, accumulator);

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void reduceWithAccumulator() {
        IntBinaryOperator accumulator = (a, b) -> b;
        OptionalInt result = this.parallelStreamSupportMock.reduce(accumulator);

        verify(this.delegateMock).reduce(accumulator);
        assertEquals(OptionalInt.of(42), result);
    }

    @Test
    public void reduceWithAccumulatorSequential() {
        this.parallelIntStreamSupport.sequential();
        IntBinaryOperator accumulator = (a, b) -> b;
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .reduce(accumulator);

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void reduceWithAccumulatorParallel() {
        this.parallelIntStreamSupport.parallel();
        IntBinaryOperator accumulator = (a, b) -> b;
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .reduce(accumulator);

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void collectWithSupplierAndAccumulatorAndCombiner() {
        Supplier<String> supplier = () -> "x";
        ObjIntConsumer<String> accumulator = (a, b) -> {
        };
        BiConsumer<String, String> combiner = (a, b) -> {
        };

        String result = this.parallelStreamSupportMock.collect(supplier, accumulator, combiner);

        verify(this.delegateMock).collect(supplier, accumulator, combiner);
        assertEquals("collect", result);
    }

    @Test
    public void collectWithSupplierAndAccumulatorAndCombinerSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void collectWithSupplierAndAccumulatorAndCombinerParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void sum() {
        int result = this.parallelStreamSupportMock.sum();

        verify(this.delegateMock).sum();
        assertEquals(42, result);
    }

    @Test
    public void sumSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .sum();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void sumParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .sum();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void min() {
        OptionalInt result = this.parallelStreamSupportMock.min();

        verify(this.delegateMock).min();
        assertEquals(OptionalInt.of(42), result);
    }

    @Test
    public void minSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .min();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void minParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .min();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void max() {
        OptionalInt result = this.parallelStreamSupportMock.max();

        verify(this.delegateMock).max();
        assertEquals(OptionalInt.of(42), result);
    }

    @Test
    public void maxSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .max();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void maxParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .max();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void count() {
        long count = this.parallelStreamSupportMock.count();

        verify(this.delegateMock).count();
        assertEquals(42L, count);
    }

    @Test
    public void countSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .filter(i -> {
                // Don't use peek() in combination with count(). See Javadoc.
                threadRef.set(currentThread());
                return true;
            }).count();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void countParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            // Don't use peek() in combination with count(). See Javadoc.
            .filter(i -> {
                threadRef.set(currentThread());
                return true;
            }).count();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void average() {
        OptionalDouble result = this.parallelStreamSupportMock.average();

        verify(this.delegateMock).average();
        assertEquals(OptionalDouble.of(42.0), result);
    }

    @Test
    public void averageSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .average();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void averageParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .average();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void summaryStatistics() {
        IntSummaryStatistics result = this.parallelStreamSupportMock.summaryStatistics();

        verify(this.delegateMock).summaryStatistics();
        assertEquals(this.summaryStatistics, result);
    }

    @Test
    public void summaryStatisticsSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .summaryStatistics();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void summaryStatisticsParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .summaryStatistics();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void anyMatch() {
        IntPredicate p = i -> true;

        boolean result = this.parallelStreamSupportMock.anyMatch(p);

        verify(this.delegateMock).anyMatch(p);
        assertTrue(result);
    }

    @Test
    public void anyMatchSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .anyMatch(i -> true);

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void anyMatchParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .anyMatch(i -> true);

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void allMatch() {
        IntPredicate p = i -> true;

        boolean result = this.parallelStreamSupportMock.allMatch(p);

        verify(this.delegateMock).allMatch(p);
        assertTrue(result);
    }

    @Test
    public void allMatchSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .allMatch(i -> true);

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void allMatchParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .allMatch(i -> true);

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void noneMatch() {
        IntPredicate p = i -> true;

        boolean result = this.parallelStreamSupportMock.noneMatch(p);

        verify(this.delegateMock).noneMatch(p);
        assertTrue(result);
    }

    @Test
    public void noneMatchSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .noneMatch(i -> true);

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void noneMatchParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .noneMatch(i -> true);

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void findFirst() {
        OptionalInt result = this.parallelStreamSupportMock.findFirst();

        verify(this.delegateMock).findFirst();
        assertEquals(OptionalInt.of(42), result);
    }

    @Test
    public void findFirstSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .findFirst();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void findFirstParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .findFirst();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void findAny() {
        OptionalInt result = this.parallelStreamSupportMock.findAny();

        verify(this.delegateMock).findAny();
        assertEquals(OptionalInt.of(42), result);
    }

    @Test
    public void findAnytSequential() {
        this.parallelIntStreamSupport.sequential();
        Thread thisThread = currentThread();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .findAny();

        assertEquals(thisThread, threadRef.get());
    }

    @Test
    public void findAnyParallel() {
        this.parallelIntStreamSupport.parallel();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        this.parallelIntStreamSupport
            .peek(i -> threadRef.set(currentThread()))
            .findAny();

        assertThat(threadRef.get(), instanceOf(ForkJoinWorkerThread.class));
    }

    @Test
    public void asLongStream() {
        LongStream stream = this.parallelStreamSupportMock.asLongStream();

        verify(this.delegateMock).asLongStream();
        assertThat(stream, instanceOf(ParallelLongStream.class));
        assertSame(this.mappedLongDelegateMock, ((ParallelLongStream) stream).delegate);
        assertSame(this.workerPool, ((ParallelLongStream) stream).workerPool);
    }

    @Test
    public void asDoubleStream() {
        DoubleStream stream = this.parallelStreamSupportMock.asDoubleStream();

        verify(this.delegateMock).asDoubleStream();
        assertThat(stream, instanceOf(ParallelDoubleStream.class));
        assertSame(this.mappedDoubleDelegateMock, ((ParallelDoubleStream) stream).delegate);
        assertSame(this.workerPool, ((ParallelDoubleStream) stream).workerPool);
    }

    @Test
    public void boxed() {
        Stream<Integer> stream = this.parallelStreamSupportMock.boxed();

        verify(this.delegateMock).boxed();
        assertThat(stream, instanceOf(ParallelStream.class));
        assertSame(this.mappedDelegateMock, ((ParallelStream) stream).delegate);
        assertSame(this.workerPool, ((ParallelStream) stream).workerPool);
    }

    @Override
    @Test
    public void iterator() {
        PrimitiveIterator.OfInt iterator = this.parallelStreamSupportMock.iterator();

        verify(this.delegateMock).iterator();
        assertSame(this.iteratorMock, iterator);
    }

    @Override
    @Test
    public void spliterator() {
        Spliterator.OfInt spliterator = this.parallelStreamSupportMock.spliterator();

        verify(this.delegateMock).spliterator();
        assertSame(this.spliteratorMock, spliterator);
    }
}
