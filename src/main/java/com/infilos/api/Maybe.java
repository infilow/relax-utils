package com.infilos.api;

import com.infilos.utils.Require;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Like Try type in Scala, declare the checked exception as a type parameter.
 *
 * Class that wraps checked exceptions and tunnels them through stream operations or future graphs.
 *
 * <p>The idea is to wrap checked exceptions inside {@code Stream<Maybe<T, E>>}, then {@code map()},
 * {@code flatMap()} and {@code filter()} away through normal stream operations. Exception is only thrown during terminal operations. For example, the following code fetches and runs pending jobs using a stream of {@code Maybe}:
 *
 * <pre>{@code
 *   private Job fetchJob(long jobId) throws IOException;
 *
 *   void runPendingJobs() throws IOException {
 *     Stream<Maybe<Job, IOException>> stream = activeJobIds.stream()
 *         .map(maybe(this::fetchJob))
 *         .filter(byValue(Job::isPending));
 *     Iterate.through(stream, m -> m.orElseThrow(IOException::new).runJob());
 *   }
 * }</pre>
 *
 * When it comes to futures, the following asynchronous code example handles exceptions type safely using {@link #catchException Maybe.catchException()}:
 * <pre>{@code
 *   CompletionStage<User> assumeAnonymousIfNotAuthenticated(CompletionStage<User> stage) {
 *     CompletionStage<Maybe<User, AuthenticationException>> authenticated =
 *         Maybe.catchException(AuthenticationException.class, stage);
 *     return authenticated.thenApply(maybe -> maybe.orElse(e -> new AnonymousUser()));
 *   }
 * }</pre>
 */
public abstract class Maybe<T, E extends Throwable> {

    /**
     * Creates a Maybe for value.
     *
     * @param value can be null
     */
    public static <T, E extends Throwable> Maybe<T, E> of(T value) {
        return new Success<>(value);
    }

    /**
     * Creates an exceptional Maybe for exception.
     *
     * <p>If exception is an {@link InterruptedException}, the current thread is
     * re-interrupted as a standard practice to avoid swallowing the interruption signal.
     */
    public static <T, E extends Throwable> Maybe<T, E> except(E exception) {
        return new Failure<>(exception);
    }

    /**
     * Map the success value with function.
     */
    public abstract <T2> Maybe<T2, E> map(Function<? super T, ? extends T2> function);

    /**
     * Flatmap the success value with function.
     */
    public abstract <T2> Maybe<T2, E> flatMap(Function<? super T, Maybe<T2, E>> function);

    /**
     * Check if is success.
     */
    public abstract boolean isPresent();

    /**
     * Consume success value.
     */
    public abstract Maybe<T, E> ifPresent(Consumer<? super T> consumer);

    /**
     * Transform failure exception to value, or return a new exception of type X.
     */
    public abstract <X extends Throwable> T getOrMap(CheckedFunction<? super E, ? extends T, X> function) throws X;

    /**
     * Get the success value or throw the original exception.
     */
    public final T getOrThrow() throws E {
        return getOrThrow(Maybe::cleanupInterruption);
    }

    /**
     * Get the success value or throw the wrappered exception, recommended way to retain the stack trace.
     *
     * eg. maybe.getOrThrow(E::new)
     */
    public final T getOrThrow(Function<? super E, ? extends E> exceptionWrapper) throws E {
        Require.checkNotNull(exceptionWrapper);

        return getOrMap(e -> {
            throw exceptionWrapper.apply(e);
        });
    }

    /**
     * <pre>
     * Catches and handles exception with handler, and then skips it in the returned Stream.
     * This is specially useful in a Stream chain to handle and then ignore exceptional results.
     * </pre>
     */
    public final <X extends Throwable> Stream<T> onFailure(CheckedConsumer<? super E, ? extends X> handler) throws X {
        Require.checkNotNull(handler);

        return map(Stream::of).getOrMap(e -> {
            handler.accept(e);
            return Stream.<T>empty();
        });
    }

    /**
     * <pre>
     * Turns condition to a Predicate over Maybe. 
     * The returned predicate matches any Maybe with a matching value, as well as any exceptional Maybe so as not to accidentally swallow exceptions.
     * </pre>
     */
    public static <T, E extends Throwable> Predicate<Maybe<T, E>> match(Predicate<? super T> condition) {
        Require.checkNotNull(condition);
        return maybe -> maybe.map(condition::test).getOrMap(e -> true);
    }

    /**
     * Create maybe from value supplier, unchecked exception will be throw.
     */
    public static <T, E extends Throwable> Maybe<T, E> wrap(CheckedSupplier<? extends T, ? extends E> supplier) {
        Require.checkNotNull(supplier);

        try {
            return of(supplier.get());
        } catch (Throwable e) {
            // CheckedSupplier<T, E> can only throw unchecked or E.
            @SuppressWarnings("unchecked")
            E exception = (E) propagateIfUnchecked(e);
            return except(exception);
        }
    }

    /**
     * Create maybe from stream supplier, unchecked exception will be throw.
     */
    public static <T, E extends Throwable> Stream<Maybe<T, E>> wrapStream(
        CheckedSupplier<? extends Stream<? extends T>, E> supplier) {
        return wrap(supplier).map(s -> s.map(Maybe::<T, E>of)).getOrMap(e -> Stream.<Maybe<T, E>>of(except(e)));
    }

    /**
     * Wraps function to be used for a stream of Maybe.
     */
    public static <F, T, E extends Throwable> Function<F, Maybe<T, E>> wrap(CheckedFunction<? super F, ? extends T, E> function) {
        Require.checkNotNull(function);
        return maybe -> wrap(() -> function.apply(maybe));
    }

    /**
     * Wraps {@code function} that returns {@code Stream<T>} to one that returns {@code Stream<Maybe<T, E>>} with exceptions of type {@code E} wrapped.
     *
     * <p>Useful to be passed to {@link Stream#flatMap}.
     *
     * <p>Unchecked exceptions will be immediately propagated without being wrapped.
     */
    public static <F, T, E extends Throwable> Function<F, Stream<Maybe<T, E>>> wrapStream(
        CheckedFunction<? super F, ? extends Stream<? extends T>, E> function) {
        Function<F, Maybe<Stream<? extends T>, E>> wrapped = wrap(function);

        return wrapped.andThen(Maybe::maybeStream);
    }

    /**
     * Wraps {@code function} to be used for a stream of Maybe.
     *
     * <p>Unchecked exceptions will be immediately propagated without being wrapped.
     */
    public static <A, B, T, E extends Throwable> BiFunction<A, B, Maybe<T, E>> wrap(
        CheckedBiFunction<? super A, ? super B, ? extends T, ? extends E> function) {
        Require.checkNotNull(function);
        return (a, b) -> wrap(() -> function.apply(a, b));
    }

    /**
     * Wraps {@code function} that returns {@code Stream<T>} to one that returns {@code Stream<Maybe<T, E>>} with exceptions of type {@code E} wrapped.
     *
     * <p>Useful to be passed to {@link Stream#flatMap}.
     *
     * <p>Unchecked exceptions will be immediately propagated without being wrapped.
     */
    public static <A, B, T, E extends Throwable> BiFunction<A, B, Stream<Maybe<T, E>>> wrapStream(
        CheckedBiFunction<? super A, ? super B, ? extends Stream<? extends T>, ? extends E> function) {
        BiFunction<A, B, Maybe<Stream<? extends T>, E>> wrapped = wrap(function);
        return wrapped.andThen(Maybe::maybeStream);
    }

    /**
     * Wraps {@code supplier} to be used for a stream of Maybe.
     *
     * <p>Normally one should use {@link #wrap(CheckedSupplier)} unless {@code E} is an unchecked
     * exception type.
     *
     * <p>For GWT code, wrap the supplier manually, as in:
     *
     * <pre>{@code
     *   private static <T> Maybe<T, FooException> foo(
     *       CheckedSupplier<T, FooException> supplier) {
     *     try {
     *       return Maybe.of(supplier.get());
     *     } catch (FooException e) {
     *       return Maybe.except(e);
     *     }
     *   }
     * }</pre>
     */
    public static <T, E extends Throwable> Maybe<T, E> wrap(CheckedSupplier<? extends T, ? extends E> supplier,
                                                            Class<E> exceptionType) {
        Require.checkNotNull(supplier);
        Require.checkNotNull(exceptionType);
        try {
            return of(supplier.get());
        } catch (Throwable e) {
            if (exceptionType.isInstance(e)) {
                return except(exceptionType.cast(e));
            }
            throw new AssertionError(propagateIfUnchecked(e));
        }
    }

    /**
     * Invokes {@code supplier} and wraps the returned {@code Stream<T>} or thrown exception into a stream of {@code Maybe<T, E>}.
     */
    public static <T, E extends Throwable> Stream<Maybe<T, E>> wrapStream(CheckedSupplier<? extends Stream<? extends T>, ? extends E> supplier,
                                                                          Class<E> exceptionType) {
        return maybeStream(wrap(supplier, exceptionType));
    }

    /**
     * Wraps {@code function} to be used for a stream of Maybe.
     *
     * <p>Normally one should use {@link #wrap(CheckedFunction)} unless {@code E} is an unchecked
     * exception type.
     *
     * <p>For GWT code, wrap the function manually, as in:
     *
     * <pre>{@code
     *   private static <F, T> Function<F, Maybe<T, FooException>> foo(
     *       CheckedFunction<F, T, FooException> function) {
     *     return from -> {
     *       try {
     *         return Maybe.of(function.apply(from));
     *       } catch (FooException e) {
     *         return Maybe.except(e);
     *       }
     *     };
     *   }
     * }</pre>
     */
    public static <F, T, E extends Throwable> Function<F, Maybe<T, E>> wrap(
        CheckedFunction<? super F, ? extends T, ? extends E> function, Class<E> exceptionType) {
        Require.checkNotNull(function);
        Require.checkNotNull(exceptionType);
        return from -> wrap(() -> function.apply(from), exceptionType);
    }

    /**
     * Wraps {@code function} that returns {@code Stream<T>} to one that returns {@code Stream<Maybe<T, E>>} with exceptions of type {@code E} wrapped.
     */
    public static <F, T, E extends Throwable> Function<F, Stream<Maybe<T, E>>> wrapStream(
        CheckedFunction<? super F, ? extends Stream<? extends T>, ? extends E> function,
        Class<E> exceptionType) {
        Function<F, Maybe<Stream<? extends T>, E>> wrapped = wrap(function, exceptionType);
        return wrapped.andThen(Maybe::maybeStream);
    }

    /**
     * Wraps {@code function} to be used for a stream of Maybe.
     *
     * <p>Normally one should use {@link #wrap(CheckedBiFunction)} unless {@code E} is an unchecked
     * exception type.
     *
     * <p>For GWT code, wrap the function manually, as in:
     *
     * <pre>{@code
     *   private static <A, B, T> BiFunction<A, B, Maybe<T, FooException>> foo(
     *       CheckedBiFunction<A, B, T, FooException> function) {
     *     return (a, b) -> {
     *       try {
     *         return Maybe.of(function.apply(a, b));
     *       } catch (FooException e) {
     *         return Maybe.except(e);
     *       }
     *     };
     *   }
     * }</pre>
     */
    public static <A, B, T, E extends Throwable> BiFunction<A, B, Maybe<T, E>> wrap(
        CheckedBiFunction<? super A, ? super B, ? extends T, ? extends E> function,
        Class<E> exceptionType) {
        Require.checkNotNull(function);
        Require.checkNotNull(exceptionType);
        return (a, b) -> wrap(() -> function.apply(a, b), exceptionType);
    }

    /**
     * Wraps {@code function} that returns {@code Stream<T>} to one that returns {@code Stream<Maybe<T, E>>} with exceptions of type {@code E} wrapped.
     */
    public static <A, B, T, E extends Throwable> BiFunction<A, B, Stream<Maybe<T, E>>> wrapStream(
        CheckedBiFunction<? super A, ? super B, ? extends Stream<? extends T>, ? extends E> function,
        Class<E> exceptionType) {
        BiFunction<A, B, Maybe<Stream<? extends T>, E>> wrapped = wrap(function, exceptionType);
        return wrapped.andThen(Maybe::maybeStream);
    }

    /**
     * Returns a wrapper of {@code stage} that if {@code stage} failed with exception of {@code exceptionType}, that exception is caught and wrapped inside a {@link Maybe} to complete the wrapper stage normally.
     *
     * <p>This is useful if the asynchronous code is interested in recovering from its own exception
     * without having to deal with other exception types.
     */
    public static <T, E extends Throwable> CompletionStage<Maybe<T, E>> catchException(Class<E> exceptionType, CompletionStage<? extends T> stage) {
        Require.checkNotNull(exceptionType);
        CompletableFuture<Maybe<T, E>> future = new CompletableFuture<>();
        stage.handle((v, e) -> {
            try {
                if (e == null) {
                    future.complete(Maybe.of(v));
                } else {
                    unwrapFutureException(exceptionType, e)
                        .map(cause -> future.complete(Maybe.except(cause)))
                        .orElseGet(() -> future.completeExceptionally(e));
                }
            } catch (Throwable x) {  // Just in case there was a bug. Don't hang the thread.
                if (x != e) x.addSuppressed(e);
                future.completeExceptionally(x);
            }
            return null;
        });
        return future;
    }

    private static <E extends Throwable> Optional<E> unwrapFutureException(
        Class<E> causeType, Throwable exception) {
        for (Throwable e = exception; ; e = e.getCause()) {
            if (causeType.isInstance(e)) {
                return Optional.of(causeType.cast(e));
            }
            if (!(e instanceof ExecutionException || e instanceof CompletionException)) {
                return Optional.empty();
            }
        }
    }

    /** Adapts a {@code Maybe<Stream<T>, E>} to {@code Stream<Maybe<T, E>}. */
    private static <T, E extends Throwable> Stream<Maybe<T, E>> maybeStream(
        Maybe<? extends Stream<? extends T>, ? extends E> maybeStream) {
        return maybeStream.map(s -> s.map(Maybe::<T, E>of)).getOrMap(e -> Stream.<Maybe<T, E>>of(except(e)));
    }

    private static <E extends Throwable> E cleanupInterruption(E exception) {
        if (exception instanceof InterruptedException) {
            Thread.interrupted();
        }
        return exception;
    }

    private static <E extends Throwable> E propagateIfUnchecked(E exception) {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else if (exception instanceof Error) {
            throw (Error) exception;
        } else {
            return exception;
        }
    }

    /** No subclasses! */
    private Maybe() {
    }

    private static final class Success<T, E extends Throwable> extends Maybe<T, E> {
        private final T value;

        Success(T value) {
            this.value = value;
        }

        @Override
        public <T2> Maybe<T2, E> map(Function<? super T, ? extends T2> f) {
            return of(f.apply(value));
        }

        @Override
        public <T2> Maybe<T2, E> flatMap(Function<? super T, Maybe<T2, E>> f) {
            return f.apply(value);
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public Maybe<T, E> ifPresent(Consumer<? super T> consumer) {
            consumer.accept(value);
            return this;
        }

        @Override
        public <X extends Throwable> T getOrMap(CheckedFunction<? super E, ? extends T, X> f)
            throws X {
            Require.checkNotNull(f);
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public int hashCode() {
            return value == null ? 0 : value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Success<?, ?>) {
                Success<?, ?> that = (Success<?, ?>) obj;
                return Objects.equals(value, that.value);
            }
            return false;
        }
    }

    private static final class Failure<T, E extends Throwable> extends Maybe<T, E> {
        private final E exception;

        Failure(E exception) {
            this.exception = Require.checkNotNull(exception);
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public <T2> Maybe<T2, E> map(Function<? super T, ? extends T2> f) {
            Require.checkNotNull(f);
            return except(exception);
        }

        @Override
        public <T2> Maybe<T2, E> flatMap(Function<? super T, Maybe<T2, E>> f) {
            Require.checkNotNull(f);
            return except(exception);
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public Maybe<T, E> ifPresent(Consumer<? super T> consumer) {
            Require.checkNotNull(consumer);
            return this;
        }

        @Override
        public <X extends Throwable> T getOrMap(CheckedFunction<? super E, ? extends T, X> f)
            throws X {
            return f.apply(exception);
        }

        @Override
        public String toString() {
            return "exception: " + exception;
        }

        @Override
        public int hashCode() {
            return exception.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Failure<?, ?>) {
                Failure<?, ?> that = (Failure<?, ?>) obj;
                return exception.equals(that.exception);
            }
            return false;
        }
    }
}
