package com.infilos;

import org.assertj.core.api.*;

import java.util.concurrent.*;

import static org.junit.Assert.assertThrows;
import static org.assertj.core.api.Assertions.*;

public class FutureAssert {
    
    public static AbstractThrowableAssert<?,Throwable> assertCauseOf(
        Class<? extends Throwable> exceptionType, CompletionStage<?> stage) {
        CompletableFuture<?> future = stage.toCompletableFuture();
        Throwable thrown = assertThrows(exceptionType, future::get);
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isTrue();
        return assertThat(thrown.getCause());
    }

    public static CancellationException assertCancelled(CompletionStage<?> stage) {
        CompletableFuture<?> future = stage.toCompletableFuture();
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isTrue();
        CancellationException cancelled = assertThrows(CancellationException.class, future::get);
        assertThat(future.isCancelled()).isTrue();
        return cancelled;
    }

    public static ObjectAssert<Object> assertCompleted(CompletionStage<?> stage)
        throws InterruptedException, ExecutionException {
        assertThat(stage.toCompletableFuture().isDone()).isTrue();
        Object result = stage.toCompletableFuture().get();
        assertThat(stage.toCompletableFuture().isCancelled()).isFalse();
        assertThat(stage.toCompletableFuture().isCompletedExceptionally()).isFalse();
        return assertThat(result);
    }

    public static ObjectAssert<Object> assertAfterCompleted(CompletionStage<?> stage)
        throws InterruptedException, ExecutionException {
        Object result = stage.toCompletableFuture().get();
        assertThat(stage.toCompletableFuture().isDone()).isTrue();
        assertThat(stage.toCompletableFuture().isCancelled()).isFalse();
        assertThat(stage.toCompletableFuture().isCompletedExceptionally()).isFalse();
        return assertThat(result);
    }

    public static void assertPending(CompletionStage<?> stage) {
        assertThat(stage.toCompletableFuture().isDone()).isFalse();
        assertThat(stage.toCompletableFuture().isCancelled()).isFalse();
        assertThat(stage.toCompletableFuture().isCompletedExceptionally()).isFalse();
    }
}
