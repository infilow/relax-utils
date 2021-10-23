package com.infilos.utils.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class CommonStream {
    private CommonStream() {
    }

    public static <T> List<List<T>> splitChunk(List<T> list, int chunkSize) {
        return splitChunk(list, chunkSize, false);
    }

    public static <T> List<List<T>> splitChunk(List<T> list, int chunkSize, boolean mergeSmallLast) {
        Objects.requireNonNull(list, "list must not be null");

        List<List<T>> chunks = IntStream.range(0, list.size() / chunkSize + 1)
            .mapToObj(i -> list.subList(i * chunkSize, Math.min((i + 1) * chunkSize, list.size())))
            .filter(c -> c.size() > 0)
            .collect(Collectors.toList());

        if (mergeSmallLast && chunks.size() > 1 && chunks.get(chunks.size() - 1).size() <= chunkSize / 2) {
            chunks.set(chunks.size() - 2, new ArrayList<T>() {{
                addAll(chunks.get(chunks.size() - 2));
                addAll(chunks.get(chunks.size() - 1));
            }});
            return chunks.subList(0, chunks.size() - 1);
        }

        return chunks;
    }
}
