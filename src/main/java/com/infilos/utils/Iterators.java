package com.infilos.utils;

import com.infilos.collect.EnumerationIterator;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class Iterators {
    private Iterators() {
    }

    public static boolean isEmpty(Iterable<?> iterable) {
        return null == iterable || isEmpty(iterable.iterator());
    }

    public static boolean isEmpty(Iterator<?> iterator) {
        return null == iterator || !iterator.hasNext();
    }

    public static boolean isNotEmpty(Iterable<?> iterable) {
        return null != iterable && isNotEmpty(iterable.iterator());
    }

    public static boolean isNotEmpty(Iterator<?> iterable) {
        return null != iterable && iterable.hasNext();
    }

    public static boolean containsNull(Iterable<?> iterable) {
        return containsNull(null == iterable ? null : iterable.iterator());
    }

    public static boolean containsNull(Iterator<?> iter) {
        if (isNotEmpty(iter)) {
            while (iter.hasNext()) {
                if (null == iter.next()) {
                    return true;
                }
            }
        }
        
        return false;
    }

    public static <T> Map<T, Integer> countMap(Iterable<T> iter) {
        return countMap(null == iter ? null : iter.iterator());
    }
    
    public static <T> Map<T, Integer> countMap(Iterator<T> iter) {
        final HashMap<T, Integer> countMap = new HashMap<>();
        if (null != iter) {
            Integer count;
            T t;
            while (iter.hasNext()) {
                t = iter.next();
                count = countMap.get(t);
                if (null == count) {
                    countMap.put(t, 1);
                } else {
                    countMap.put(t, count + 1);
                }
            }
        }
        return countMap;
    }

    public static <T> String join(Iterable<T> iterable, CharSequence conjunction) {
        if (null == iterable) {
            return null;
        }
        return join(iterable.iterator(), conjunction);
    }

    public static <T> String join(Iterator<T> iterator, CharSequence conjunction) {
        if (null == iterator) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        T item;
        while (iterator.hasNext()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(conjunction);
            }

            item = iterator.next();
            if (Arrays.isArray(item)) {
                sb.append(java.util.Arrays.stream(Arrays.wrap(item)).map(Object::toString).collect(Collectors.joining(conjunction)));
            } else if (item instanceof Iterable<?>) {
                sb.append(join((Iterable<?>) item, conjunction));
            } else if (item instanceof Iterator<?>) {
                sb.append(join((Iterator<?>) item, conjunction));
            } else {
                sb.append(item);
            }
        }
        return sb.toString();
    }

    public static <K, V> HashMap<K, V> toMap(Iterable<Map.Entry<K, V>> entryIter) {
        final HashMap<K, V> map = new HashMap<>();
        if (isNotEmpty(entryIter)) {
            for (Map.Entry<K, V> entry : entryIter) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    public static <K, V> Map<K, V> toMap(Iterable<K> keys, Iterable<V> values) {
        return toMap(null == keys ? null : keys.iterator(), null == values ? null : values.iterator());
    }

    public static <K, V> Map<K, V> toMap(Iterator<K> keys, Iterator<V> values) {
        final Map<K, V> resultMap = new HashMap<>();
        if (isNotEmpty(keys)) {
            while (keys.hasNext()) {
                resultMap.put(keys.next(), (null != values && values.hasNext()) ? values.next() : null);
            }
        }
        return resultMap;
    }

    public static <E> Iterator<E> create(Enumeration<E> e) {
        return new EnumerationIterator<>(e);
    }

    public static <E> Iterable<E> createIterable(final Iterator<E> iter) {
        return () -> iter;
    }

    public static <T> T getFirst(Iterable<T> iterable) {
        if (null != iterable) {
            return getFirst(iterable.iterator());
        }
        return null;
    }

    public static <T> T getFirst(Iterator<T> iterator) {
        if (null != iterator && iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    public static Class<?> getElementType(Iterable<?> iterable) {
        if (null != iterable) {
            final Iterator<?> iterator = iterable.iterator();
            return getElementType(iterator);
        }
        return null;
    }

    public static Class<?> getElementType(Iterator<?> iterator) {
        if (null != iterator) {
            Object t;
            while (iterator.hasNext()) {
                t = iterator.next();
                if (null != t) {
                    return t.getClass();
                }
            }
        }
        return null;
    }
}
