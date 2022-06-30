package com.infilos.utils;

import com.infilos.reflect.TypeHelper;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class Threads {
    private Threads() {
    }

    public static void keep() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {
        }
    }

    public static void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ignore) {
        }
    }

    public static CompletionStage<?> ifCancelled(CompletionStage<?> stage, Consumer<? super CancellationException> action) {
        Require.checkNotNull(action);
        
        return stage.exceptionally(e -> {
            TypeHelper.cast(e, CancellationException.class).ifPresent(action);
            return null;
        });
    }

    /** Propagates cancellation from {@code outer} to {@code inner}. */
    public static <T> CompletionStage<T> propagateCancellation(CompletionStage<T> outer, CompletionStage<?> inner) {
        Require.checkNotNull(inner);

        Threads.ifCancelled(outer, e -> {
            // Even if this isn't supported, the worst is that we don't propagate cancellation.
            // But that's fine because without a Future we cannot propagate anyway.
            inner.toCompletableFuture().completeExceptionally(e);
        });

        return outer;
    }
}