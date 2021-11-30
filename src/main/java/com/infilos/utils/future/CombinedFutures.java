package com.infilos.utils.future;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;

public final class CombinedFutures {

    private static final Object NULL_PLACEHOLDER = new Object();

    private final IdentityHashMap<CompletionStage<?>, Object> map = new IdentityHashMap<>();

    public CombinedFutures(List<? extends CompletionStage<?>> stages) {
        for (final CompletionStage<?> stage : stages) {
            Object value = stage.toCompletableFuture().join();
            if (value==null) {
                value = NULL_PLACEHOLDER;
            }
            map.put(stage, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(CompletionStage<T> stage) {
        final Object value = map.get(stage);
        if (value==null) {
            throw new IllegalArgumentException("Can not resolve values for futures that were not part of the combine");
        }
        if (value==NULL_PLACEHOLDER) {
            return null;
        }

        return (T) value;
    }
}
