package com.infilos.utils.stream;

import com.infilos.utils.Threads;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StreamBatchTest {

    private static final ForkJoinPool pool2 = new ForkJoinPool(2);

    private static final Random random = new Random();
    private static final Function<List<Integer>, String> executor = value -> {
        Threads.sleep(random.nextInt(3));

        String result = value.stream().map(String::valueOf).collect(Collectors.joining(","));
        System.out.printf("Execute Chunk(%s): %s%n", System.currentTimeMillis(), result);

        return result;
    };

    @Test
    public void test() {
        executeParallel(executor, oneTo(10), 3, pool2);
    }

    private <I, R> List<R> executeParallel(Function<List<I>, R> executor, List<I> items, int chunckSize, ForkJoinPool pool) {
        System.out.println("Start:");
        if (items.size() < chunckSize) {
            return Collections.singletonList(executor.apply(items));
        }

        List<List<I>> chunks = CommonStream.splitChunk(items, chunckSize);

        return ParallelStream.submit(chunks, pool).map(executor).collect(Collectors.toList());
    }

    private static List<Integer> oneTo(Integer stop) {
        return IntStream.range(1, stop +1).boxed().collect(Collectors.toList());
    }
}
