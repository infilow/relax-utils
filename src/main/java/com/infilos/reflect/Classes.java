package com.infilos.reflect;

import java.util.Optional;

public final class Classes {
    private Classes() {
    }

    /**
     * Casts object to type if it's a non-null instance of T, or else returns Optional.empty().
     */
    public static <T> Optional<T> cast(Object object, Class<T> type) {
        return type.isInstance(object) ? Optional.of(type.cast(object)) : Optional.empty();
    }
}
