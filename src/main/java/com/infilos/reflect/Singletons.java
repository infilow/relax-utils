package com.infilos.reflect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton instance container.
 */
public final class Singletons {
    private Singletons() {
    }

    private static final Map<Class<?>, Object> POOL = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz, Object... params) {
        T obj = (T) POOL.get(clazz);

        if (null == obj) {
            synchronized (Singletons.class) {
                obj = (T) POOL.get(clazz);
                if (null == obj) {
                    obj = ClassHelper.createInstance(clazz, params);
                    POOL.put(clazz, obj);
                }
            }
        }

        return obj;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String className, Object... params) {
        final Class<T> clazz = (Class<T>) ClassLoaders.loadClass(className);
        return get(clazz, params);
    }

    public static void remove(Class<?> clazz) {
        POOL.remove(clazz);
    }

    public static void destroy() {
        POOL.clear();
    }
}
