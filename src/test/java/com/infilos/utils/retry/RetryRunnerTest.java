package com.infilos.utils.retry;

import com.infilos.api.CheckedSupplier;
import com.infilos.utils.Require;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.*;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.*;

import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;
import static com.infilos.FutureAssert.*;
import static java.util.concurrent.CompletableFuture.completedFuture;

@RunWith(JUnit4.class)
public class RetryRunnerTest {

    @Spy private FakeClock clock;
    @Spy private FakeScheduledExecutorService executor;
    @Mock private Action action;
    private final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();
    private RetryRunner retryer = new RetryRunner();

    @Before
    public void setUpMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void noMoreInteractions() {
        Mockito.verifyNoMoreInteractions(action);
    }

    @Test
    public void cannotRetryOnInterruptedException() {
        assertThrows(IllegalArgumentException.class, () -> upon(InterruptedException.class, emptyList()));
        assertThrows(
            IllegalArgumentException.class,
            () -> retryer.upon(InterruptedException.class, e -> false, emptyList())
        );
    }

    @Test
    public void cannotRetryOnSubtypeOfInterruptedException() {
        class MyInterruptedException extends InterruptedException {
        }
        assertThrows(
            IllegalArgumentException.class, () -> upon(MyInterruptedException.class, emptyList()));
        assertThrows(
            IllegalArgumentException.class,
            () -> retryer.upon(MyInterruptedException.class, e -> false, emptyList()));
    }

    @Test
    public void expectedReturnValueFirstTime() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        when(action.run()).thenReturn("good");
        
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        assertCompleted(forReturnValue.retry(action::run, executor)).isEqualTo("good");
        verify(action).run();
        verify(delay, never()).beforeDelay(any());
        verify(delay, never()).afterDelay(any());
    }

    @Test
    public void nullReturnValueIsGood() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        when(action.run()).thenReturn(null);
        
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        assertCompleted(forReturnValue.retry(action::run, executor)).isNull();
        verify(action).run();
        verify(delay, never()).beforeDelay(any());
        verify(delay, never()).afterDelay(any());
    }

    @Test
    public void nullReturnValueRetried() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn(null, asList(delay));
        when(action.run()).thenReturn(null).thenReturn("fixed");
        
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).run();
        verify(delay).beforeDelay(null);
        verify(delay).afterDelay(null);
    }

    @Test
    public void errorPropagatedDuringReturnValueRetry() throws Exception {
        Error error = new Error("test");
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        when(action.run()).thenThrow(error);
        
        assertException(Error.class, () -> forReturnValue.retry(action::run, executor))
            .isSameAs(error);
        assertThat(error.getSuppressed()).isEmpty();
        verify(action).run();
        verify(delay, never()).beforeDelay(any());
        verify(delay, never()).afterDelay(any());
    }

    @Test
    public void uncheckedExceptionPropagatedDuringReturnValueRetry() throws Exception {
        RuntimeException error = new RuntimeException("test");
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        when(action.run()).thenThrow(error);
        
        assertException(RuntimeException.class, () -> forReturnValue.retry(action::run, executor))
            .isSameAs(error);
        assertThat(error.getSuppressed()).isEmpty();
        verify(action).run();
        verify(delay, never()).beforeDelay(any());
        verify(delay, never()).afterDelay(any());
    }

    @Test
    public void exceptionFromBeforeDelayReportedDuringReturnValueRetry() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        when(action.run()).thenReturn("bad");
        RuntimeException unexpected = new RuntimeException();
        Mockito.doThrow(unexpected).when(delay).beforeDelay("bad");
        
        assertException(RuntimeException.class, () -> forReturnValue.retry(action::run, executor))
            .isSameAs(unexpected);
        assertThat(unexpected.getSuppressed()).isEmpty();
        verify(action).run();
        verify(delay).beforeDelay("bad");
        verify(delay, never()).afterDelay("bad");
    }

    @Test
    public void exceptionFromAfterDelayReportedDuringReturnValueRetry() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        when(action.run()).thenReturn("bad");
        RuntimeException unexpected = new RuntimeException();
        Mockito.doThrow(unexpected).when(delay).afterDelay("bad");
        
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCauseOf(ExecutionException.class, stage).isSameAs(unexpected);
        assertThat(unexpected.getSuppressed()).isEmpty();
        verify(action).run();
        verify(delay).beforeDelay("bad");
        verify(delay).afterDelay("bad");
    }

    @Test
    public void exceptionFromExecutorReportedDuringReturnValueRetry() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        when(action.run()).thenReturn("bad");
        RejectedExecutionException unexpected = new RejectedExecutionException();
        Mockito.doThrow(unexpected)
            .when(executor).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        assertException(
            RejectedExecutionException.class, () -> forReturnValue.retry(action::run, executor))
            .isSameAs(unexpected);
        assertThat(unexpected.getSuppressed()).isEmpty();
        verify(action).run();
        verify(delay).beforeDelay("bad");
        verify(delay, never()).afterDelay("bad");
    }

    @Test
    public void returnValueScheduledForRetry() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        when(action.run()).thenReturn("bad");
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(Duration.ofMillis(999));
        assertPending(stage);
        verify(action).run();
        verify(delay).beforeDelay("bad");
        verify(delay, never()).afterDelay(any());
    }

    @Test
    public void returnValueRetriedButCancelled() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(0));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        when(action.run()).thenReturn("bad").thenReturn("fixed");
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        stage.toCompletableFuture().cancel(true);
        assertThat(scheduledFutures).hasSize(1);
        verify(scheduledFutures.get(0)).cancel(true);
        CancellationException cancelled = assertCancelled(stage);
        assertThat(cancelled.getSuppressed()).isEmpty();
        verify(action).run();
        verify(delay).beforeDelay("bad");

        // Cancelled so no more retry.
        elapse(Duration.ofSeconds(100));
        verifyNoMoreInteractions(action);
    }

    @Test
    public void returnValueRetried() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay));
        when(action.run()).thenReturn("bad").thenReturn("fixed");
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).run();
        verify(delay).beforeDelay("bad");
        verify(delay).afterDelay("bad");
    }

    @Test
    public void returnValueRetriedToNoAvail() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer.uponReturn("bad", asList(delay, delay));
        when(action.run()).thenReturn("bad");
        
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("bad");
        verify(action, times(3)).run();
        verify(delay, times(2)).beforeDelay("bad");
        verify(delay, times(2)).afterDelay("bad");
    }

    @Test
    public void returnValueRetrialExceedsTime() throws Exception {
        RetryOnReturn<String> forReturnValue = retryer.uponReturn(
            "bad", ofSeconds(4).timed(Collections.nCopies(100, ofSeconds(1)), clock));
        when(action.run()).thenReturn("bad").thenReturn("bad").thenReturn("bad").thenReturn("good");
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(Duration.ofSeconds(2));
        assertPending(stage);
        elapse(Duration.ofSeconds(1));  // exceeds deadline
        assertCompleted(stage).isEqualTo("bad");
        verify(action, times(3)).run();  // Retry twice.
    }

    @Test
    public void returnValueAsyncRetriedToSuccess() throws Exception {
        RetryOnReturn<String> forReturnValue = retryer.uponReturn(
            "bad", ofSeconds(1).withBackoff(2, 1));
        when(action.runAsync())
            .thenReturn(completedFuture("bad"))
            .thenReturn(completedFuture("fixed"));
        CompletionStage<String> stage = forReturnValue.retryAsync(action::runAsync, executor);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).runAsync();
    }

    @Test
    public void returnValueAsyncFailedAfterRetry() throws Exception {
        RetryDelay<String> delay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue =
            retryer.ifReturns((String s) -> s.startsWith("bad"), asList(delay));
        when(action.runAsync())
            .thenReturn(completedFuture("bad"))
            .thenReturn(completedFuture("bad2"));
        CompletionStage<String> stage = forReturnValue.retryAsync(action::runAsync, executor);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("bad2");
        verify(action, times(2)).runAsync();
        verify(delay).beforeDelay("bad");
        verify(delay).afterDelay("bad");
    }

    @Test
    public void testCustomDelayForReturnValueRetry() throws Exception {
        TestDelay<String> delay = new TestDelay<String>() {
            @Override
            public Duration duration() {
                return Duration.ofMillis(1);
            }
        };
        RetryOnReturn<String> forReturnValue =
            retryer.ifReturns(s -> s.startsWith("bad"), asList(delay).stream());
        when(action.run()).thenReturn("bad").thenReturn("fixed");
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        elapse(Duration.ofMillis(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).run();
        assertThat(delay.before).isEqualTo("bad");
        assertThat(delay.after).isEqualTo("bad");
    }

    @Test
    public void actionSucceedsFirstTime() throws Exception {
        when(action.run()).thenReturn("good");
        assertCompleted(retry(action::run)).isEqualTo("good");
        verify(action).run();
    }

    @Test
    public void errorPropagated() throws Exception {
        Error error = new Error("test");
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        when(action.run()).thenThrow(error);
        assertException(Error.class, () -> retry(action::run)).isSameAs(error);
        assertThat(error.getSuppressed()).isEmpty();
        verify(action).run();
        verify(delay, never()).beforeDelay(any());
        verify(delay, never()).afterDelay(any());
    }

    @Test
    public void uncheckedExceptionPropagated() throws Exception {
        RuntimeException error = new RuntimeException("test");
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        when(action.run()).thenThrow(error);
        assertException(RuntimeException.class, () -> retry(action::run)).isSameAs(error);
        assertThat(error.getSuppressed()).isEmpty();
        verify(action).run();
        verify(delay, never()).beforeDelay(any());
        verify(delay, never()).afterDelay(any());
    }

    @Test
    public void actionFailedButNoRetry() throws Exception {
        IOException exception = new IOException("bad");
        when(action.run()).thenThrow(exception);
        assertCauseOf(ExecutionException.class, retry(action::run)).isSameAs(exception);
        assertThat(exception.getSuppressed()).isEmpty();
        verify(action).run();
    }

    @Test
    public void exceptionFromBeforeDelayPropagated() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        IOException exception = new IOException();
        when(action.run()).thenThrow(exception);
        RuntimeException unexpected = new RuntimeException();
        Mockito.doThrow(unexpected).when(delay).beforeDelay(any());
        assertException(RuntimeException.class, () -> retry(action::run))
            .isSameAs(unexpected);
        assertThat(unexpected.getSuppressed()).asList().containsExactly(exception);
        verify(action).run();
        verify(delay).beforeDelay(exception);
        verify(delay, never()).afterDelay(exception);
    }

    @Test
    public void exceptionFromAfterDelayResultsInExecutionException() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        IOException exception = new IOException();
        when(action.run()).thenThrow(exception);
        RuntimeException unexpected = new RuntimeException();
        Mockito.doThrow(unexpected).when(delay).afterDelay(any());
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCauseOf(ExecutionException.class, stage).isSameAs(unexpected);
        assertThat(unexpected.getSuppressed()).asList().containsExactly(exception);
        verify(action).run();
        verify(delay).beforeDelay(exception);
        verify(delay).afterDelay(exception);
    }

    @Test
    public void exceptionFromExecutorPropagated() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        IOException exception = new IOException();
        when(action.run()).thenThrow(exception);
        RejectedExecutionException unexpected = new RejectedExecutionException();
        Mockito.doThrow(unexpected)
            .when(executor).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        assertException(RejectedExecutionException.class, () -> retry(action::run))
            .isSameAs(unexpected);
        assertThat(unexpected.getSuppressed()).asList().containsExactly(exception);
        verify(action).run();
        verify(delay).beforeDelay(exception);
        verify(delay, never()).afterDelay(exception);
    }

    @Test
    public void actionFailedAndScheduledForRetry() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        IOException exception = new IOException();
        when(action.run()).thenThrow(exception);
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(Duration.ofMillis(999));
        assertPending(stage);
        verify(action).run();
        verify(delay).beforeDelay(exception);
        verify(delay, never()).afterDelay(any());
    }

    @Test
    public void actionNotScheduledForRetryDueToCancellation() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        CompletableFuture<String> result = new CompletableFuture<>();
        when(action.runAsync()).thenReturn(result);
        CompletionStage<String> stage = retryAsync(action::runAsync);
        assertPending(stage);
        stage.toCompletableFuture().cancel(false);
        IOException exception = new IOException();
        result.completeExceptionally(exception);
        CancellationException cancelled = assertCancelled(stage);
        assertThat(cancelled.getSuppressed()).asList().containsExactly(exception);
        verify(action).runAsync();
        verify(executor, never()).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        verify(delay, never()).beforeDelay(exception);
        verify(delay, never()).afterDelay(exception);
    }

    @Test
    public void actionRetriedButCancelled() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        IOException exception = new IOException();
        when(action.run()).thenThrow(exception).thenReturn("fixed");
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        stage.toCompletableFuture().cancel(false);
        assertThat(scheduledFutures).hasSize(1);
        verify(scheduledFutures.get(0)).cancel(true);
        CancellationException cancelled = assertCancelled(stage);
        assertThat(cancelled.getSuppressed()).asList().containsExactly(exception);
        verify(action).run();
        verify(delay).beforeDelay(exception);

        // Cancelled so no more retry.
        elapse(Duration.ofSeconds(100));
        verifyNoMoreInteractions(action);
    }

    @Test
    public void actionFailedAndRetriedToSuccess() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        IOException exception = new IOException();
        when(action.run()).thenThrow(exception).thenReturn("fixed");
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).run();
        verify(delay).beforeDelay(exception);
        verify(delay).afterDelay(exception);
    }

    @Test
    public void errorRetried() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(MyError.class, asList(delay));
        MyError error = new MyError("test");
        when(action.run()).thenThrow(error).thenReturn("fixed");
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).run();
        verify(delay).beforeDelay(error);
        verify(delay).afterDelay(error);
    }

    @Test
    public void uncheckedExceptionRetried() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(RuntimeException.class, asList(delay));
        RuntimeException exception = new RuntimeException("test");
        when(action.run()).thenThrow(exception).thenReturn("fixed");
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).run();
        verify(delay).beforeDelay(exception);
        verify(delay).afterDelay(exception);
    }

    @Test
    public void actionFailedAfterRetry() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        IOException firstException = new IOException();
        IOException exception = new IOException("hopeless");
        when(action.run()).thenThrow(firstException).thenThrow(exception);
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
        assertThat(exception.getSuppressed()).asList().containsExactly(firstException);
        verify(action, times(2)).run();
        verify(delay).beforeDelay(firstException);
        verify(delay).afterDelay(firstException);
    }

    @Test
    public void retrialExceedsTime() throws Exception {
        upon(
            IOException.class,
            ofSeconds(4).timed(Collections.nCopies(100, ofSeconds(1)), clock));
        IOException exception1 = new IOException();
        IOException exception = new IOException("hopeless");
        when(action.run())
            .thenThrow(exception1).thenThrow(exception).thenThrow(exception).thenReturn("good");
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(Duration.ofSeconds(2));
        assertPending(stage);
        elapse(Duration.ofSeconds(1));  // exceeds time
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
        assertThat(exception.getSuppressed()).asList().containsExactly(exception1);
        verify(action, times(3)).run();  // Retry twice.
    }

    @Test
    public void asyncExceptionRetriedToSuccess() throws Exception {
        upon(IOException.class, ofSeconds(1).withBackoff(2, 1));
        when(action.runAsync())
            .thenReturn(exceptionally(new IOException()))
            .thenReturn(completedFuture("fixed"));
        CompletionStage<String> stage = retryAsync(action::runAsync);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).runAsync();
    }

    @Test
    public void asyncFailedAfterRetry() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay));
        IOException firstException = new IOException();
        IOException exception = new IOException("hopeless");
        when(action.runAsync())
            .thenReturn(exceptionally(firstException))
            .thenReturn(exceptionally(exception));
        CompletionStage<String> stage = retryAsync(action::runAsync);
        assertPending(stage);
        elapse(Duration.ofSeconds(1));
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
        verify(action, times(2)).runAsync();
        verify(delay).beforeDelay(firstException);
        verify(delay).afterDelay(firstException);
    }

    @Test
    public void twoDifferentExceptionRulesRetriedToSuccess() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay, delay));
        upon(MyError.class, asList(delay));
        IOException exception = new IOException();
        MyError error = new MyError("test");
        when(action.run()).thenThrow(exception).thenThrow(error).thenThrow(exception).thenReturn("fixed");
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(4, Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(4)).run();
        verify(delay, times(2)).beforeDelay(exception);
        verify(delay, times(2)).afterDelay(exception);
        verify(delay).beforeDelay(error);
        verify(delay).afterDelay(error);
    }

    @Test
    public void twoDifferentExceptionRulesRetriedAndFailed() throws Exception {
        RetryDelay<Throwable> delay = spy(ofSeconds(1));
        upon(IOException.class, asList(delay, delay));
        upon(MyError.class, asList(delay));
        IOException exception1 = new IOException();
        MyError error2 = new MyError("test");
        IOException exception3 = new IOException();
        MyError error4 = new MyError("test");
        when(action.run()).thenThrow(exception1).thenThrow(error2).thenThrow(exception3)
            .thenThrow(error4);
        CompletionStage<String> stage = retry(action::run);
        assertPending(stage);
        elapse(4, Duration.ofSeconds(1));
        assertCauseOf(ExecutionException.class, stage).isSameAs(error4);
        assertThat(error4.getSuppressed()).asList().containsExactly(exception1, error2, exception3);
        assertThat(error4.getCause()).isNull();
        assertThat(exception3.getSuppressed()).isEmpty();
        assertThat(error2.getSuppressed()).isEmpty();
        assertThat(exception1.getSuppressed()).isEmpty();
        verify(action, times(4)).run();
        verify(delay).beforeDelay(exception1);
        verify(delay).afterDelay(exception1);
        verify(delay).beforeDelay(error2);
        verify(delay).afterDelay(error2);
        verify(delay).beforeDelay(exception3);
        verify(delay).afterDelay(exception3);
    }

    @Test
    public void returnValueAndExceptionRetryToSuccess() throws Exception {
        RetryDelay<Throwable> exceptionDelay = spy(ofSeconds(1));
        RetryDelay<String> returnValueDelay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer
            .upon(IOException.class, asList(exceptionDelay))
            .uponReturn("bad", asList(returnValueDelay, returnValueDelay));
        IOException exception = new IOException();
        when(action.run())
            .thenReturn("bad").thenThrow(exception).thenReturn("bad").thenReturn("fixed");
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(4, Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(4)).run();
        verify(returnValueDelay, times(2)).beforeDelay("bad");
        verify(returnValueDelay, times(2)).afterDelay("bad");
        verify(exceptionDelay).beforeDelay(exception);
        verify(exceptionDelay).afterDelay(exception);
    }

    @Test
    public void returnValueAndExceptionRetriedButStillReturnBad() throws Exception {
        RetryDelay<Throwable> exceptionDelay = spy(ofSeconds(1));
        RetryDelay<String> returnValueDelay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer
            .upon(IOException.class, asList(exceptionDelay))
            .uponReturn("bad", asList(returnValueDelay, returnValueDelay));
        IOException exception = new IOException();
        when(action.run())
            .thenReturn("bad").thenThrow(exception).thenReturn("bad").thenReturn("bad")
            .thenReturn("fixed");
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(4, Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("bad");
        verify(action, times(4)).run();
        verify(returnValueDelay, times(2)).beforeDelay("bad");
        verify(returnValueDelay, times(2)).afterDelay("bad");
        verify(exceptionDelay).beforeDelay(exception);
        verify(exceptionDelay).afterDelay(exception);
    }

    @Test
    public void returnValueAndExceptionRetriedButStillThrows() throws Exception {
        RetryDelay<Throwable> exceptionDelay = spy(ofSeconds(1));
        RetryDelay<String> returnValueDelay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer
            .upon(IOException.class, asList(exceptionDelay))
            .uponReturn("bad", asList(returnValueDelay, returnValueDelay));
        IOException exception1 = new IOException();
        IOException exception = new IOException();
        when(action.run())
            .thenReturn("bad").thenThrow(exception1).thenReturn("bad").thenThrow(exception)
            .thenReturn("fixed");
        CompletionStage<String> stage = forReturnValue.retry(action::run, executor);
        assertPending(stage);
        elapse(4, Duration.ofSeconds(1));
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
        assertThat(exception.getSuppressed()).asList().containsExactly(exception1);
        verify(action, times(4)).run();
        verify(returnValueDelay, times(2)).beforeDelay("bad");
        verify(returnValueDelay, times(2)).afterDelay("bad");
        verify(exceptionDelay).beforeDelay(exception1);
        verify(exceptionDelay).afterDelay(exception1);
    }

    @Test
    public void returnValueAndExceptionAsyncRetryToSuccess() throws Exception {
        RetryDelay<Throwable> exceptionDelay = spy(ofSeconds(1));
        RetryDelay<String> returnValueDelay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer
            .upon(IOException.class, asList(exceptionDelay))
            .uponReturn("bad", asList(returnValueDelay, returnValueDelay));
        IOException exception = new IOException();
        when(action.runAsync())
            .thenReturn(completedFuture("bad"))
            .thenReturn(exceptionally(exception))
            .thenReturn(completedFuture("bad"))
            .thenReturn(completedFuture("fixed"));
        CompletionStage<String> stage = forReturnValue.retryAsync(action::runAsync, executor);
        assertPending(stage);
        elapse(4, Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(4)).runAsync();
        verify(returnValueDelay, times(2)).beforeDelay("bad");
        verify(returnValueDelay, times(2)).afterDelay("bad");
        verify(exceptionDelay).beforeDelay(exception);
        verify(exceptionDelay).afterDelay(exception);
    }

    @Test
    public void returnValueAndExceptionAsyncRetriedButStillReturnBad() throws Exception {
        RetryDelay<Throwable> exceptionDelay = spy(ofSeconds(1));
        RetryDelay<String> returnValueDelay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer
            .upon(IOException.class, asList(exceptionDelay))
            .uponReturn("bad", asList(returnValueDelay, returnValueDelay));
        IOException exception = new IOException();
        when(action.runAsync())
            .thenReturn(completedFuture("bad"))
            .thenThrow(exception)
            .thenReturn(completedFuture("bad"))
            .thenReturn(completedFuture("bad"))
            .thenReturn(completedFuture("fixed"));
        CompletionStage<String> stage = forReturnValue.retryAsync(action::runAsync, executor);
        assertPending(stage);
        elapse(4, Duration.ofSeconds(1));
        assertCompleted(stage).isEqualTo("bad");
        verify(action, times(4)).runAsync();
        verify(returnValueDelay, times(2)).beforeDelay("bad");
        verify(returnValueDelay, times(2)).afterDelay("bad");
        verify(exceptionDelay).beforeDelay(exception);
        verify(exceptionDelay).afterDelay(exception);
    }

    @Test
    public void returnValueAndExceptionAsyncRetriedButStillThrows() throws Exception {
        RetryDelay<Throwable> exceptionDelay = spy(ofSeconds(1));
        RetryDelay<String> returnValueDelay = spy(ofSeconds(1));
        RetryOnReturn<String> forReturnValue = retryer
            .upon(IOException.class, asList(exceptionDelay))
            .uponReturn("bad", asList(returnValueDelay, returnValueDelay));
        IOException exception1 = new IOException();
        IOException exception = new IOException();
        when(action.runAsync())
            .thenReturn(completedFuture("bad"))
            .thenReturn(exceptionally(exception1))
            .thenReturn(completedFuture("bad"))
            .thenThrow(exception)
            .thenReturn(completedFuture("fixed"));
        CompletionStage<String> stage = forReturnValue.retryAsync(action::runAsync, executor);
        assertPending(stage);
        elapse(4, Duration.ofSeconds(1));
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
        assertThat(exception.getSuppressed()).asList().containsExactly(exception1);
        verify(action, times(4)).runAsync();
        verify(returnValueDelay, times(2)).beforeDelay("bad");
        verify(returnValueDelay, times(2)).afterDelay("bad");
        verify(exceptionDelay).beforeDelay(exception1);
        verify(exceptionDelay).afterDelay(exception1);
    }

    @Test
    public void testCustomDelay() throws Exception {
        TestDelay<IOException> delay = new TestDelay<IOException>() {
            @Override
            public Duration duration() {
                return Duration.ofMillis(1);
            }
        };
        upon(IOException.class, asList(delay).stream());  // to make sure the stream overload works.
        IOException exception = new IOException();
        when(action.run()).thenThrow(exception).thenReturn("fixed");
        CompletionStage<String> stage = retry(action::run);
        elapse(Duration.ofMillis(1));
        assertCompleted(stage).isEqualTo("fixed");
        verify(action, times(2)).run();
        assertThat(delay.before).isSameAs(exception);
        assertThat(delay.after).isSameAs(exception);
    }

    @Test
    public void testImmutable() throws IOException {
        retryer.upon(IOException.class, asList(ofSeconds(1)));  // Should have no effect
        IOException exception = new IOException("bad");
        when(action.run()).thenThrow(exception);
        assertCauseOf(ExecutionException.class, retry(action::run)).isSameAs(exception);
        verify(action).run();
    }

    @Test
    public void testTimed() {
        List<RetryDelay<?>> delays = asList(1L, 8L, 1L).stream()
            .map(RetryDelay::ofMillis)
            .collect(toList());
        List<RetryDelay<?>> timed = RetryDelay.ofMillis(10).timed(delays, clock);
        assertThat(timed).hasSize(3);
        assertThat(timed).isNotEmpty();
        assertThat(timed).containsExactlyInAnyOrderElementsOf(delays);
        elapse(Duration.ofMillis(1));
        assertThat(timed).containsExactlyInAnyOrderElementsOf(delays);
        elapse(Duration.ofMillis(1));
        assertThat(timed.get(0)).isEqualTo(delays.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> timed.get(1));
    }

    @Test
    public void testNulls() throws Exception {
        //Stream<?> statelessStream = (Stream<?>) Proxy.newProxyInstance(
        //    RetryRunnerTest.class.getClassLoader(), new Class<?>[]{Stream.class},
        //    (p, method, args) -> method.invoke(Stream.of(), args));
        //new NullPointerTester()
        //    .setDefault(Stream.class, statelessStream)
        //    .ignore(RetryRunner.class.getMethod("uponReturn", Object.class, Stream.class))
        //    .ignore(RetryRunner.class.getMethod("uponReturn", Object.class, List.class))
        //    .testAllPublicInstanceMethods(new Retryer());
    }

    @Test
    public void testForReturnValue_nulls() {
        //new NullPointerTester()
        //    .testAllPublicInstanceMethods(new Retryer().uponReturn("bad", asList()));
    }

    @Test
    public void testDelay_nulls() {
        //new NullPointerTester().testAllPublicStaticMethods(Delay.class);
        //new NullPointerTester()
        //    .setDefault(Clock.class, Clock.systemUTC())
        //    .testAllPublicInstanceMethods(new ExceptionDelay());
    }

    @Test
    public void testDelay_multiplied() {
        assertThat(ofDays(1).multipliedBy(0)).isEqualTo(ofDays(0));
        assertThat(ofDays(2).multipliedBy(1)).isEqualTo(ofDays(2));
        assertThat(ofDays(3).multipliedBy(2)).isEqualTo(ofDays(6));
        assertThrows(IllegalArgumentException.class, () -> ofDays(1).multipliedBy(-1));
        assertThat(ofDays(1).multipliedBy(Double.MIN_VALUE)).isEqualTo(RetryDelay.ofMillis(1));
    }

    @Test
    public void testDelay_withBackoff() {
        assertThat(ofDays(1).withBackoff(2, 3))
            .containsExactly(ofDays(1), ofDays(2), ofDays(4));
        assertThat(ofDays(1).withBackoff(1, 2))
            .containsExactly(ofDays(1), ofDays(1));
        assertThat(ofDays(1).withBackoff(1, 0)).isEmpty();
        assertThrows(IllegalArgumentException.class, () -> ofDays(1).withBackoff(0, 1));
        assertThrows(IllegalArgumentException.class, () -> ofDays(1).withBackoff(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> ofDays(1).withBackoff(2, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> ofDays(1).withBackoff(1, 1).get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> ofDays(1).withBackoff(1, 1).get(1));
    }

    @Test
    public void testDelay_fibonacci() {
        assertThat(ofDays(1).fibonacci(1)).containsExactly(ofDays(1));
        assertThat(ofDays(1).fibonacci(2)).containsExactly(ofDays(1), ofDays(1));
        assertThat(ofDays(1).fibonacci(3)).containsExactly(ofDays(1), ofDays(1), ofDays(2));
        assertThat(ofDays(1).fibonacci(5))
            .containsExactly(ofDays(1), ofDays(1), ofDays(2), ofDays(3), ofDays(5));
        assertThat(ofDays(1).fibonacci(500).get(499)).isEqualTo(RetryDelay.ofMillis(Long.MAX_VALUE));
        assertThat(ofDays(1).fibonacci(0)).isEmpty();
        assertThrows(IllegalArgumentException.class, () -> ofDays(1).fibonacci(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> ofDays(1).fibonacci(1).get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> ofDays(1).fibonacci(1).get(1));
    }

    @Test
    public void testDelay_randomized_invalid() {
        assertThrows(IllegalArgumentException.class, () -> ofDays(1).randomized(new Random(), -0.1));
        assertThrows(IllegalArgumentException.class, () -> ofDays(1).randomized(new Random(), 1.1));
    }

    @Test
    public void testDelay_randomized_zeroRandomness() {
        RetryDelay<?> delay = ofDays(1).randomized(new Random(), 0);
        assertThat(delay).isEqualTo(ofDays(1));
    }

    @Ignore("Can't mock Random in JDK 17")
    @Test
    public void testDelay_randomized_halfRandomness() {
        Random random = Mockito.mock(Random.class);
        when(random.nextDouble()).thenReturn(0D).thenReturn(0.5D).thenReturn(1D);
        assertThat(ofDays(1).randomized(random, 0.5).duration()).isEqualTo(Duration.ofHours(12));
        assertThat(ofDays(1).randomized(random, 0.5).duration()).isEqualTo(Duration.ofHours(24));
        assertThat(ofDays(1).randomized(random, 0.5).duration()).isEqualTo(Duration.ofHours(36));
    }

    @Ignore("Can't mock Random in JDK 17")
    @Test
    public void testDelay_randomized_fullRandomness() {
        Random random = Mockito.mock(Random.class);
        when(random.nextDouble()).thenReturn(0D).thenReturn(0.5D).thenReturn(1D);
        assertThat(ofDays(1).randomized(random, 1).duration()).isEqualTo(Duration.ofHours(0));
        assertThat(ofDays(1).randomized(random, 1).duration()).isEqualTo(Duration.ofHours(24));
        assertThat(ofDays(1).randomized(random, 1).duration()).isEqualTo(Duration.ofHours(48));
    }

    @Test
    public void testDelay_equals() {
        //new EqualsTester()
        //    .addEqualityGroup(
        //        RetryDelay.ofMillis(1000),
        //        Delay.of(Duration.ofMillis(1000)),
        //        Delay.of(Duration.ofSeconds(1)))
        //    .addEqualityGroup(RetryDelay.ofMillis(2))
        //    .testEquals();
    }

    @Test
    public void testDelay_compareTo() {
        assertThat(RetryDelay.ofMillis(1)).isLessThan(RetryDelay.ofMillis(2));
        assertThat(RetryDelay.ofMillis(1)).isGreaterThan(RetryDelay.ofMillis(0));
        assertThat(RetryDelay.ofMillis(1)).isEqualByComparingTo(RetryDelay.ofMillis(1));
    }

    @Test
    public void testDelay_of() {
        assertThat(RetryDelay.ofMillis(Long.MAX_VALUE).duration())
            .isEqualTo(Duration.ofMillis(Long.MAX_VALUE));
        assertThat(RetryDelay.ofMillis(0).duration()).isEqualTo(Duration.ofMillis(0));
        assertThat(RetryDelay.ofMillis(1).duration()).isEqualTo(Duration.ofMillis(1));
        assertThat(ofDays(0).duration()).isEqualTo(Duration.ofDays(0));
        assertThat(ofDays(1).duration()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    public void testDelay_invalid() {
        assertThrows(RuntimeException.class, () -> ofDays(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> RetryDelay.ofMillis(-1));
        assertThrows(RuntimeException.class, () -> RetryDelay.ofMillis(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> RetryDelay.of(Duration.ofDays(-1)));
    }

    @Test
    public void testDelay_forEvents() {
        RetryDelay<String> delay = spy(new SpyableDelay<String>(Duration.ofDays(1)));
        RetryDelay<Integer> mapped = delay.forEvents(Object::toString);
        assertThat(mapped).isEqualTo(delay);
        mapped.beforeDelay(123);
        verify(delay).beforeDelay("123");
        mapped.afterDelay(456);
        verify(delay).afterDelay("456");
    }

    @Test
    public void testFakeScheduledExecutorService_taskScheduledButNotRunYet() {
        Runnable runnable = mock(Runnable.class);
        executor.schedule(runnable, 2, TimeUnit.MILLISECONDS);
        elapse(Duration.ofMillis(1));
        Mockito.verifyNoInteractions(runnable);
    }

    @Test
    public void testFakeScheduledExecutorService_taskScheduledAndRun() {
        Runnable runnable = mock(Runnable.class);
        executor.schedule(runnable, 2, TimeUnit.MILLISECONDS);
        elapse(Duration.ofMillis(2));
        verify(runnable).run();
        elapse(Duration.ofMillis(2));
        Mockito.verifyNoMoreInteractions(runnable);
    }

    @Test
    public void testFakeScheduledExecutorService_taskScheduleAnotherTask() {
        Runnable runnable = mock(Runnable.class);
        Runnable scheduled = () -> executor.schedule(runnable, 3, TimeUnit.MILLISECONDS);
        executor.schedule(scheduled, 2, TimeUnit.MILLISECONDS);
        elapse(Duration.ofMillis(2));
        elapse(Duration.ofMillis(3));
        verify(runnable).run();
        Mockito.verifyNoMoreInteractions(runnable);
    }

    @Test
    public void testFibonacci() {
        assertThat(Math.round(RetryDelay.fib(0))).isEqualTo(0);
        assertThat(Math.round(RetryDelay.fib(1))).isEqualTo(1);
        
        List<Long> results = new ArrayList<Long>(){{
            add(0L);
            add(1L);
        }};
        
        for (int i = 2; i < 93; i++) {
            long f = Math.round(RetryDelay.fib(i));
            assertThat(f).isLessThan(Long.MAX_VALUE);
            //assertThat((double) f)
            //    .isWithin(f / 1000).of(results.get(i - 2).doubleValue() + results.get(i - 1).doubleValue());
            assertThat((double) f)
                .isCloseTo(f / 1000, within(results.get(i - 2).doubleValue() + results.get(i - 1).doubleValue()));
            System.out.printf("f: %s, f/1000: %s, within: %s%n", f, f/1000, results.get(i - 2).doubleValue() + results.get(i - 1).doubleValue());
            results.add(f);
        }
    }
    

    private static CompletionStage<String> exceptionally(Throwable e) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }

    private static <E> RetryDelay<E> ofSeconds(long seconds) {
        return new SpyableDelay<>(Duration.ofSeconds(seconds));
    }

    private static <E> RetryDelay<E> ofDays(long days) {
        return new SpyableDelay<>(Duration.ofDays(days));
    }

    private <E extends Throwable> void upon(
        Class<E> exceptionType, List<? extends RetryDelay<? super E>> delays) {
        retryer = retryer.upon(exceptionType, delays);
    }

    private <E extends Throwable> void upon(
        Class<E> exceptionType, Stream<? extends RetryDelay<? super E>> delays) {
        retryer = retryer.upon(exceptionType, delays);
    }

    private <T> CompletionStage<T> retry(CheckedSupplier<T, ?> supplier) {
        return retryer.retry(supplier, executor);
    }

    private <T> CompletionStage<T> retryAsync(
        CheckedSupplier<? extends CompletionStage<T>, ?> supplier) {
        return retryer.retryAsync(supplier, executor);
    }

    //private static ThrowableSubject assertException(Class<? extends Throwable> exceptionType, Executable executable) {
    private static AbstractThrowableAssert<?, Throwable> assertException(Class<? extends Throwable> exceptionType, ThrowingRunnable executable) {
        Throwable thrown = assertThrows(exceptionType, executable);
        return assertThat(thrown);
    }

    private void elapse(int counts, Duration duration) {
        for (int i = 0; i < counts; i++) {
            elapse(duration);
        }
    }

    private void elapse(Duration duration) {
        clock.elapse(duration);
        executor.tick();
    }

    abstract class TestDelay<E> extends RetryDelay<E> {
        E before;
        E after;

        @Override
        public void beforeDelay(E exception) {
            before = exception;
        }

        @Override
        public void afterDelay(E exception) {
            after = exception;
        }
    }

    abstract static class FakeClock extends Clock {
        private Instant now = Instant.ofEpochMilli(123456789L);

        @Override
        public Instant instant() {
            return now;
        }

        void elapse(Duration duration) {
            now = now.plus(duration);
        }
    }

    abstract class FakeScheduledExecutorService implements ScheduledExecutorService {
        private List<Schedule> schedules = new ArrayList<>();

        void tick() {
            Instant now = clock.instant();
            List<Schedule> ready =
                schedules.stream().filter(s -> s.ready(now)).collect(toList());
            schedules = schedules.stream()
                .filter(s -> s.pending(now))
                .collect(toCollection(ArrayList::new));
            ready.forEach(s -> s.command.run());
        }

        @Override
        public void execute(Runnable command) {
            schedule(command, 1, TimeUnit.MILLISECONDS);
        }

        @Override
        public ScheduledFuture<?> schedule(
            Runnable command, long delay, TimeUnit unit) {
            assertThat(unit).isEqualTo(TimeUnit.MILLISECONDS);
            schedules.add(new Schedule(clock.instant().plus(delay, ChronoUnit.MILLIS), command));
            return addScheduledFuture();
        }

        private <T> ScheduledFuture<T> addScheduledFuture() {
            @SuppressWarnings("unchecked")  // mock is safe.
            ScheduledFuture<T> scheduled = Mockito.mock(ScheduledFuture.class);
            scheduledFutures.add(scheduled);
            return scheduled;
        }

        @Deprecated  // Should not accidentally call this one since we don't use it.
        @Override
        public <V> ScheduledFuture<V> schedule(
            Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class Schedule {
        private final Instant time;
        final Runnable command;

        Schedule(Instant time, Runnable command) {
            this.time = Require.checkNotNull(time);
            this.command = Require.checkNotNull(command);
        }

        boolean ready(Instant now) {
            return !pending(now);
        }

        boolean pending(Instant now) {
            return now.isBefore(time);
        }
    }

    private interface Action {
        String run() throws IOException;

        CompletionStage<String> runAsync() throws IOException;
    }

    @SuppressWarnings("serial")
    private static final class MyError extends Error {
        MyError(String message) {
            super(message);
        }
    }

    private static final class ExceptionDelay extends RetryDelay<Throwable> {
        @Override
        public Duration duration() {
            return Duration.ofMillis(1);
        }

        @Override
        public void beforeDelay(Throwable exception) {
            Require.checkNotNull(exception);
        }

        @Override
        public void afterDelay(Throwable exception) {
            Require.checkNotNull(exception);
        }
    }
}