package com.infilos.jmh;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.infilos.utils.Futures;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@SuppressWarnings("unused")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class FuturesOfListBenchmark {

    @State(Scope.Benchmark)
    public static class Input {

        @Param({"4", "16", "64", "256", "1024"})
        int inputSize;

        List<CompletionStage<String>> stages;

        @Setup
        public void setup() {
            stages = Collections.nCopies(inputSize, completedFuture("hello"));
        }
    }

    @Benchmark
    public List<String> actual(final Input input) throws Exception {
        final List<CompletionStage<String>> stages = input.stages;
        final CompletableFuture<List<String>> future = Futures.ofList(stages);
        return future.get();
    }

    @Benchmark
    public List<String> stream(final Input input) throws Exception {
        final List<CompletionStage<String>> stages = input.stages;

        @SuppressWarnings("unchecked") // generic array creation
        final CompletableFuture<String>[] all = stages.stream()
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new);
        final CompletableFuture<List<String>> future = CompletableFuture.allOf(all)
            .thenApply(i -> Stream.of(all)
                .map(CompletableFuture::join)
                .collect(toList()));

        return future.get();
    }

    @Benchmark
    public List<String> instantiateAndFor(final Input input) throws Exception {
        final List<CompletionStage<String>> stages = input.stages;

        @SuppressWarnings("unchecked") // generic array creation
        final CompletableFuture<String>[] all = new CompletableFuture[stages.size()];
        for (int i = 0; i < stages.size(); i++) {
            all[i] = stages.get(i).toCompletableFuture();
        }
        final CompletableFuture<List<String>> future = CompletableFuture.allOf(all)
            .thenApply(ignored -> {
                final List<String> result = new ArrayList<>(all.length);
                for (int i = 0; i < all.length; i++) {
                    result.add(all[i].join());
                }
                return result;
            });

        return future.get();
    }

    @Benchmark
    public List<String> instantiateAndForeach(final Input input) throws Exception {
        final List<CompletionStage<String>> stages = input.stages;

        @SuppressWarnings("unchecked") // generic array creation
        final CompletableFuture<String>[] all = new CompletableFuture[stages.size()];
        for (int i = 0; i < stages.size(); i++) {
            all[i] = stages.get(i).toCompletableFuture();
        }

        final CompletableFuture<List<String>> future = CompletableFuture.allOf(all)
            .thenApply(ignored -> {
                final List<String> result = new ArrayList<>(all.length);
                for (CompletableFuture<String> entry : all) {
                    result.add(entry.join());
                }
                return result;
            });

        return future.get();
    }

    public static void main(String[] args) throws Exception {
        final Options opt = new OptionsBuilder()
            .include(FuturesOfListBenchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}
