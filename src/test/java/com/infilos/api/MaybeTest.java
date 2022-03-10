package com.infilos.api;

import com.infilos.utils.stream.Streams;
import org.assertj.core.api.ListAssert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static com.infilos.FutureAssert.*;

public class MaybeTest {
    
    @Test
    public void testOfNull() throws Throwable {
        assertThat(Maybe.of(null).getOrThrow()).isNull();
        assertThat(Maybe.of(null).toString()).isEqualTo("null");
    }

    @Test
    public void testOrElseThrow_success() throws Throwable {
        assertThat(Maybe.of("test").getOrThrow()).isEqualTo("test");
    }

    @Test
    public void testOrElseThrow_failure() throws Throwable {
        MyException exception = new MyException("test");
        Maybe<?, MyException> maybe = Maybe.except(exception);
        MyException thrown = assertThrows(MyException.class, maybe::getOrThrow);
        assertThat(thrown).isSameAs(exception);
    }

    @Test
    public void testOrElseThrow_failureWithCause() throws Throwable {
        MyException exception = new MyException("test");
        Exception cause = new RuntimeException();
        exception.initCause(cause);
        Maybe<?, MyException> maybe = Maybe.except(exception);
        MyException thrown = assertThrows(MyException.class, maybe::getOrThrow);
        assertThat(thrown).isSameAs(exception);
    }

    @Test
    public void testOrElseThrow_interruptedException() throws Throwable {
        Maybe<?, InterruptedException> maybe = Maybe.except(new InterruptedException());
        assertThat(Thread.interrupted()).isTrue();
        Thread.currentThread().interrupt();
        InterruptedException interrupted = assertThrows(InterruptedException.class, maybe::getOrThrow);
        assertThat(interrupted.getCause()).isNull();
        assertThat(Thread.interrupted()).isFalse();
    }

    @Test
    public void testOrElseThrow_explicitException_success() throws Throwable {
        assertThat(Maybe.of("test").getOrThrow(IOException::new)).isEqualTo("test");
    }

    @Test
    public void testOrElseThrow_explicitException_failure() throws Throwable {
        MyException exception = new MyException("test");
        Maybe<?, MyException> maybe = Maybe.except(exception);
        MyException thrown = assertThrows(MyException.class, () -> maybe.getOrThrow(MyException::new));
        assertSame(exception, thrown.getCause());
    }

    @Test
    public void testMap_success() {
        Maybe<Integer, MyException> maybe = Maybe.of(1);
        assertThat(maybe.map(Object::toString)).isEqualTo(Maybe.of("1"));
    }

    @Test
    public void testMap_failure() throws Throwable {
        MyException exception = new MyException("test");
        Maybe<?, MyException> maybe = Maybe.except(exception).map(Object::toString);
        MyException thrown = assertThrows(MyException.class, maybe::getOrThrow);
        assertThat(thrown).isSameAs(exception);
    }

    @Test
    public void testFlatMap_success() {
        Maybe<Integer, MyException> maybe = Maybe.of(1);
        assertThat(maybe.flatMap(o -> Maybe.of(o.toString()))).isEqualTo(Maybe.of("1"));
    }

    @Test
    public void testFlatMap_failure() throws Throwable {
        MyException exception = new MyException("test");
        Maybe<?, MyException> maybe = Maybe.except(exception).flatMap(o -> Maybe.of(o.toString()));
        MyException thrown = assertThrows(MyException.class, maybe::getOrThrow);
        assertThat(thrown).isSameAs(exception);
        assertThat(thrown.getSuppressed()).isEmpty();
    }

    @Test
    public void testIsPresent() {
        assertThat(Maybe.of(1).isPresent()).isTrue();
        assertThat(Maybe.except(new Exception()).isPresent()).isFalse();
    }

    @Test
    public void testIfPresent_success() {
        AtomicInteger succeeded = new AtomicInteger();
        Maybe.of(100).ifPresent(succeeded::set);
        assertThat(succeeded.get()).isEqualTo(100);
    }

    @Test
    public void testIfPresent_failure() {
        AtomicBoolean succeeded = new AtomicBoolean(false);
        Maybe.except(new Exception()).ifPresent(i -> succeeded.set(true));
        assertThat(succeeded.get()).isFalse();
    }

    @Test
    public void testOrElse() {
        assertThat(Maybe.of("good").getOrMap(Throwable::getMessage)).isEqualTo("good");
        assertThat(Maybe.except(new Exception("bad")).getOrMap(Throwable::getMessage)).isEqualTo("bad");
    }

    @Test
    public void testCatching_success() {
        AtomicReference<Throwable> failed = new AtomicReference<>();

        Maybe.of(100).onFailure(failed::set);
        assertThat(failed.get()).isNull();
    }

    @Test
    public void testCatching_failure() {
        MyException exception = new MyException("test");
        AtomicReference<Throwable> failed = new AtomicReference<>();

        Maybe.except(exception).onFailure(failed::set);
        assertThat(failed.get()).isSameAs(exception);
    }

    //@Test
    //public void testNulls_staticMethods() {
    //    for (Method method : Maybe.class.getMethods()) {
    //        if (method.isSynthetic()) continue;
    //        if (method.getName().equals("of")) continue;
    //        if (Modifier.isStatic(method.getModifiers())) {
    //            new NullPointerTester().testMethod(null, method);
    //        }
    //    }
    //}

    //@Test
    //public void testNulls_instanceMethods() throws Exception {
    //    new ClassSanityTester()
    //        .forAllPublicStaticMethods(Maybe.class)
    //        .testNulls();
    //}

    //@Test
    //public void testEquals() {
    //    Exception exception = new Exception();
    //    new EqualsTester()
    //        .addEqualityGroup(Maybe.of(1), Maybe.of(1))
    //        .addEqualityGroup(Maybe.of(null), Maybe.of(null))
    //        .addEqualityGroup(Maybe.of(2))
    //        .addEqualityGroup(Maybe.except(exception), Maybe.except(exception))
    //        .addEqualityGroup(Maybe.except(new RuntimeException()))
    //        .testEquals();
    //}

    @Test
    public void testStream_success() throws MyException {
        assertStream(Stream.of("hello", "friend").map(Maybe.wrap(this::justReturn)))
            .containsExactly("hello", "friend");
    }

    @Test
    public void testStreamFlatMap_success() throws MyException {
        assertStream(Stream.of("hello", "friend").flatMap(Maybe.wrapStream(this::streamOf)))
            .containsExactly("hello", "friend");
    }

    @Test
    public void testStream_exception() {
        Stream<Maybe<String, MyException>> stream =
            Stream.of("hello", "friend").map(Maybe.wrap(this::raise));
        assertThrows(MyException.class, () -> collect(stream));
    }

    @Test
    public void testStreamFlatMap_exception() {
        Stream<Maybe<String, MyException>> stream =
            Stream.of("hello", "friend").flatMap(Maybe.wrapStream(this::raiseForStream));
        assertThrows(MyException.class, () -> collect(stream));
    }

    @Test
    public void testStream_interrupted() {
        Stream<Maybe<String, InterruptedException>> stream =
            Stream.of(1, 2).map(Maybe.wrap(x -> hibernate()));
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class, () -> collect(stream));
        } finally {
            assertThat(Thread.interrupted()).isFalse();
        }
    }

    @Test
    public void testStream_uncheckedExceptionNotCaptured() {
        Stream<String> stream = Stream.of("hello", "friend")
            .map(Maybe.wrap(this::raiseUnchecked))
            .flatMap(m -> m.onFailure(e -> {
            }));
        assertThrows(RuntimeException.class, () -> stream.collect(toList()));
    }

    @Test
    public void testStream_swallowException() {
        assertThat(Stream.of("hello", "friend")
            .map(Maybe.wrap(this::raise))
            .flatMap(m -> m.onFailure(e -> {
            }))
            .collect(toList()))
            .isEmpty();
    }

    @Test
    public void testStream_generateSuccess() {
        assertThat(Stream.generate(() -> Maybe.wrap(() -> justReturn("good"))).findFirst().get())
            .isEqualTo(Maybe.of("good"));
    }

    @Test
    public void testStream_generateFailure() {
        Maybe<String, MyException> maybe =
            Stream.generate(() -> Maybe.wrap(() -> raise("bad"))).findFirst().get();
        assertThat(assertThrows(MyException.class, maybe::getOrThrow).getMessage()).isEqualTo("bad");
    }

    @Test
    public void testFilterByValue_successValueFiltered() throws MyException {
        assertStream(Stream.of("hello", "friend")
            .map(Maybe.wrap(this::justReturn))
            .filter(Maybe.match("hello"::equals)))
            .containsExactly("hello");
    }

    @Test
    public void testFilterByValue_failuresNotFiltered() {
        List<Maybe<String, MyException>> maybes = Stream.of("hello", "friend")
            .map(Maybe.wrap(this::raise))
            .filter(Maybe.match(s -> false))
            .collect(toList());
        assertThat(maybes).hasSize(2);
        assertThat(assertThrows(MyException.class, () -> maybes.get(0).getOrThrow()).getMessage())
            .isEqualTo("hello");
        assertThat(assertThrows(MyException.class, () -> maybes.get(1).getOrThrow()).getMessage())
            .isEqualTo("friend");
    }

    @Test
    public void wrapFuture_futureIsSuccess() throws Exception {
        CompletionStage<Maybe<String, Exception>> stage =
            Maybe.catchException(Exception.class, completedFuture("good"));
        assertCompleted(stage).isEqualTo(Maybe.of("good"));
    }

    @Test
    public void wrapFuture_futureIsSuccessNull() throws Exception {
        CompletionStage<Maybe<String, Exception>> stage =
            Maybe.catchException(Exception.class, completedFuture(null));
        assertThat(completedFuture(null).isDone()).isTrue();
        assertCompleted(stage).isEqualTo(Maybe.of(null));
    }

    @Test
    public void wrapFuture_futureIsExpectedFailure() throws Exception {
        MyException exception = new MyException("test");
        CompletionStage<Maybe<String, MyException>> stage =
            Maybe.catchException(MyException.class, exceptionally(exception));
        assertCompleted(stage).isEqualTo(Maybe.except(exception));
    }

    @Test
    public void wrapFuture_futureIsExpectedFailureNestedInExecutionException() throws Exception {
        MyUncheckedException exception = new MyUncheckedException("test");
        CompletionStage<Maybe<String, MyUncheckedException>> stage =
            Maybe.catchException(MyUncheckedException.class, executionExceptionally(exception));
        assertCompleted(stage).isEqualTo(Maybe.except(exception));
    }

    @Test
    public void wrapFuture_futureIsUnexpectedFailure() throws Exception {
        RuntimeException exception = new RuntimeException("test");
        CompletionStage<Maybe<String, MyException>> stage =
            Maybe.catchException(MyException.class, exceptionally(exception));
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
    }

    @Test
    public void wrapFuture_futureIsCancelledWithInterruption() throws Exception {
        CompletionStage<Maybe<String, MyException>> stage =
            Maybe.catchException(MyException.class, cancelled(true));
        assertCauseOf(CancellationException.class, stage);
    }

    @Test
    public void wrapFuture_futureIsCancelledWithNoInterruption() throws Exception {
        CompletionStage<Maybe<String, MyException>> stage =
            Maybe.catchException(MyException.class, cancelled(false));
        assertCauseOf(CancellationException.class, stage);
    }

    @Test
    public void wrapFuture_futureIsUnexpectedCheckedException_idempotence() throws Exception {
        MyException exception = new MyException("test");
        CompletionStage<?> stage =
            Maybe.catchException(IOException.class, exceptionally(exception));
        stage = Maybe.catchException(IOException.class, stage);
        stage = Maybe.catchException(MyUncheckedException.class, stage);
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
    }

    @Test
    public void wrapFuture_futureIsUnexpectedUncheckedException_idempotence() throws Exception {
        RuntimeException exception = new RuntimeException("test");
        CompletionStage<?> stage =
            Maybe.catchException(IOException.class, exceptionally(exception));
        stage = Maybe.catchException(IOException.class, stage);
        stage = Maybe.catchException(MyException.class, stage);
        stage = Maybe.catchException(Error.class, stage);
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
    }

    @Test
    public void wrapFuture_futureIsUnexpectedError_idempotence() throws Exception {
        Error error = new Error("test");
        CompletionStage<?> stage =
            Maybe.catchException(IOException.class, exceptionally(error));
        stage = Maybe.catchException(IOException.class, stage);
        stage = Maybe.catchException(MyException.class, stage);
        stage = Maybe.catchException(MyUncheckedException.class, stage);
        assertCauseOf(ExecutionException.class, stage).isSameAs(error);
    }

    @Test
    public void wrapFuture_futureIsUnexpectedFailure_notApplied() throws Exception {
        RuntimeException exception = new RuntimeException("test");
        CompletionStage<?> stage = exceptionally(exception);
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
    }

    @Test
    public void wrapFuture_futureBecomesSuccess() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletionStage<Maybe<String, Exception>> stage = Maybe.catchException(Exception.class, future);
        assertPending(stage);
        future.complete("good");
        assertCompleted(stage).isEqualTo(Maybe.of("good"));
    }

    @Test
    public void wrapFuture_futureBecomesExpectedFailure() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletionStage<Maybe<String, MyException>> stage =
            Maybe.catchException(MyException.class, future);
        assertPending(stage);
        MyException exception = new MyException("test");
        future.completeExceptionally(exception);
        assertCompleted(stage).isEqualTo(Maybe.except(exception));
    }

    @Test
    public void wrapFuture_transparentToHandle() throws Exception {
        assertCompleted(naiveExceptionHandlingCode(exceptionalUserCode())).isNull();
        assertCompleted(naiveExceptionHandlingCode(
            Maybe.catchException(MyUncheckedException.class, exceptionalUserCode())))
            .isNull();
    }

    @Test
    public void wrapFuture_transparentToExceptionally() throws Exception {
        assertCompleted(naiveExceptionallyCode(exceptionalUserCode())).isNull();
        assertCompleted(naiveExceptionallyCode(
            Maybe.catchException(MyUncheckedException.class, exceptionalUserCode())))
            .isNull();
    }

    private static CompletionStage<String> exceptionalUserCode() {
        CompletableFuture<String> future = new CompletableFuture<>();
        MyException exception = new MyException("test");
        future.completeExceptionally(exception);
        return future;
    }

    private static <T> CompletionStage<T> naiveExceptionHandlingCode(
        CompletionStage<T> stage) {
        return stage.handle((v, e) -> {
            assertThat(e).isInstanceOf(MyException.class);
            return null;
        });
    }

    private static <T> CompletionStage<T> naiveExceptionallyCode(
        CompletionStage<T> stage) {
        return stage.exceptionally(e -> {
            assertThat(e).isInstanceOf(MyException.class);
            return null;
        });
    }

    @Test
    public void testCompletionStage_handle_wraps() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        MyException exception = new MyException("test");
        future.completeExceptionally(exception);
        CompletionStage<String> stage = future.handle((v, e) -> {
            throw new CompletionException(e);
        });
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
    }

    @Test
    public void testCompletionStage_exceptionally_wraps() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        MyException exception = new MyException("test");
        future.completeExceptionally(exception);
        CompletionStage<String> stage = future.exceptionally(e -> {
            throw new CompletionException(e);
        });
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
    }

    @Test
    public void wrapFuture_futureBecomesUnexpectedFailure() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletionStage<Maybe<String, MyException>> stage =
            Maybe.catchException(MyException.class, future);
        assertPending(stage);
        RuntimeException exception = new RuntimeException("test");
        future.completeExceptionally(exception);
        assertCauseOf(ExecutionException.class, stage).isSameAs(exception);
    }

    @Test
    public void testExecutionExceptionally() {
        RuntimeException exception = new RuntimeException("test");
        assertCauseOf(ExecutionException.class, executionExceptionally(exception))
            .isSameAs(exception);
    }

    private static <T> CompletionStage<T> exceptionally(Throwable e) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }

    private static <T> CompletionStage<T> cancelled(boolean mayInterruptIfRunning) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.cancel(mayInterruptIfRunning);
        return future;
    }

    private static <T> CompletionStage<T> executionExceptionally(RuntimeException e) {
        return completedFuture((T) null).whenComplete((v, x) -> {
            throw e;
        });
    }

    private String raise(String s) throws MyException {
        throw new MyException(s);
    }

    private Stream<String> raiseForStream(String s) throws MyException {
        throw new MyException(s);
    }

    @SuppressWarnings("unused")  // Signature needed for Maybe.wrap()
    private String raiseUnchecked(String s) throws MyException {
        throw new RuntimeException(s);
    }

    @SuppressWarnings("unused")  // Signature needed for Maybe.wrap()
    private String justReturn(String s) throws MyException {
        return s;
    }

    @SuppressWarnings("unused")  // Signature needed for Maybe.wrap()
    private Stream<String> streamOf(String s) throws MyException {
        return Stream.of(s);
    }

    private static <T, E extends Throwable> ListAssert<T> assertStream(Stream<Maybe<T, E>> stream) throws E {
        return assertThat(collect(stream));
    }

    private static <T, E extends Throwable> List<T> collect(Stream<Maybe<T, E>> stream) throws E {
        List<T> list = new ArrayList<>();
        Streams.iterateThrough(stream, m -> list.add(m.getOrThrow()));
        return list;
    }

    private static String hibernate() throws InterruptedException {
        new CountDownLatch(1).await();
        throw new AssertionError("can't reach here");
    }


    @SuppressWarnings("serial")
    private static class MyException extends Exception {
        MyException(String message) {
            super(message);
        }

        MyException(Throwable cause) {
            super(cause);
        }
    }

    @SuppressWarnings("serial")
    private static class MyUncheckedException extends RuntimeException {
        MyUncheckedException(String message) {
            super(message);
        }
    }
}