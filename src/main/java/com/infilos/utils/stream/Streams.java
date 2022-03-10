package com.infilos.utils.stream;

import com.infilos.api.CheckedConsumer;
import com.infilos.utils.Require;

import java.util.stream.Stream;

public class Streams {

    /**
     * Iterates through {@code stream} <em>only once</em>. It's strongly recommended to avoid assigning the return value to a variable or passing it to any other method because the returned {@code Iterable}'s {@link Iterable#iterator
     * iterator()} method can only be called once. Instead, always use it together with a for-each loop, as in:
     *
     * <pre>{@code
     *   for (Foo foo : iterateOnce(stream)) {
     *     ...
     *     if (...) continue;
     *     if (...) break;
     *     ...
     *   }
     * }</pre>
     *
     * The above is equivalent to manually doing:
     *
     * <pre>{@code
     *   Iterable<Foo> foos = stream::iterator;
     *   for (Foo foo : foos) {
     *     ...
     *   }
     * }</pre>
     * except using this API eliminates the need for a named variable that escapes the scope of the for-each loop. And code is more readable too.
     *
     * <p>Note that {@link #iterateThrough iterateThrough()} should be preferred whenever possible
     * due to the caveats mentioned above. This method is still useful when the loop body needs to use control flows such as {@code break} or {@code return}.
     */
    public static <T> Iterable<T> iterateOnce(Stream<T> stream) {
        return stream::iterator;
    }

    /**
     * Iterates through {@code stream} sequentially and passes each element to {@code consumer} with exceptions propagated. For example:
     *
     * <pre>{@code
     *   void writeAll(Stream<?> stream, ObjectOutput out) throws IOException {
     *     iterateThrough(stream, out::writeObject);
     *   }
     * }</pre>
     */
    public static <T, E extends Throwable> void iterateThrough(
        Stream<? extends T> stream, CheckedConsumer<? super T, E> consumer) throws E {
        Require.checkNotNull(consumer);
        for (T element : iterateOnce(stream)) {
            consumer.accept(element);
        }
    }
}
