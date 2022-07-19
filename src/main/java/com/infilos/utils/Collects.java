package com.infilos.utils;

import com.infilos.reflect.ClassHelper;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
public final class Collects {
    private Collects() {
    }

    public static <T> Collection<T> union(final Collection<T> coll1, final Collection<T> coll2) {
        final ArrayList<T> list = new ArrayList<>();
        if (isEmpty(coll1)) {
            list.addAll(coll2);
        } else if (isEmpty(coll2)) {
            list.addAll(coll1);
        } else {
            final Map<T, Integer> map1 = countMap(coll1);
            final Map<T, Integer> map2 = countMap(coll2);
            final Set<T> elts = createHashSet(coll2);
            elts.addAll(coll1);
            int m;
            for (T t : elts) {
                m = Math.max(
                    Optional.ofNullable(map1.get(t)).orElse(0),
                    Optional.ofNullable(map2.get(t)).orElse(0)
                );
                for (int i = 0; i < m; i++) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    @SafeVarargs
    public static <T> Collection<T> union(final Collection<T> coll1, final Collection<T> coll2, final Collection<T>... otherColls) {
        Collection<T> union = union(coll1, coll2);
        for (Collection<T> coll : otherColls) {
            union = union(union, coll);
        }
        return union;
    }

    public static <T> Collection<T> intersect(final Collection<T> coll1, final Collection<T> coll2) {
        final ArrayList<T> list = new ArrayList<>();
        if (isNotEmpty(coll1) && isNotEmpty(coll2)) {
            final Map<T, Integer> map1 = countMap(coll1);
            final Map<T, Integer> map2 = countMap(coll2);
            final Set<T> elts = createHashSet(coll2);
            int m;
            for (T t : elts) {
                m = Math.min(
                    Optional.ofNullable(map1.get(t)).orElse(0),
                    Optional.ofNullable(map2.get(t)).orElse(0)
                );
                for (int i = 0; i < m; i++) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    @SafeVarargs
    public static <T> Collection<T> intersect(final Collection<T> coll1, final Collection<T> coll2, final Collection<T>... otherColls) {
        Collection<T> intersection = intersect(coll1, coll2);
        if (isEmpty(intersection)) {
            return intersection;
        }
        for (Collection<T> coll : otherColls) {
            intersection = intersect(intersection, coll);
            if (isEmpty(intersection)) {
                return intersection;
            }
        }
        return intersection;
    }

    public static <T> Collection<T> disjunct(final Collection<T> coll1, final Collection<T> coll2) {
        final ArrayList<T> list = new ArrayList<>();
        if (isNotEmpty(coll1) && isNotEmpty(coll2)) {
            final Map<T, Integer> map1 = countMap(coll1);
            final Map<T, Integer> map2 = countMap(coll2);
            final Set<T> elts = createHashSet(coll2);
            elts.addAll(coll1);
            int m;
            for (T t : elts) {
                m = Math.abs(Optional.ofNullable(map1.get(t)).orElse(0) - Optional.ofNullable(map2.get(t)).orElse(0));
                for (int i = 0; i < m; i++) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    public static boolean containsAny(final Collection<?> coll1, final Collection<?> coll2) {
        if (isEmpty(coll1) || isEmpty(coll2)) {
            return false;
        }
        if (coll1.size() < coll2.size()) {
            for (Object object : coll1) {
                if (coll2.contains(object)) {
                    return true;
                }
            }
        } else {
            for (Object object : coll2) {
                if (coll1.contains(object)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <T> Map<T, Integer> countMap(Iterable<T> collection) {
        return Iterators.countMap(collection);
    }

    public static <T> String join(Iterable<T> iterable, CharSequence conjunction) {
        return Iterators.join(iterable, conjunction);
    }

    public static <T> String join(Iterator<T> iterator, CharSequence conjunction) {
        return Iterators.join(iterator, conjunction);
    }

    public static <K, V> HashMap<K, V> createHashMap() {
        return Maps.create();
    }

    public static <K, V> HashMap<K, V> createHashMap(int size, boolean isOrder) {
        return Maps.create(size, isOrder);
    }

    public static <K, V> HashMap<K, V> createHashMap(int size) {
        return Maps.create(size);
    }

    @SafeVarargs
    public static <T> HashSet<T> createHashSet(T... ts) {
        return createHashSet(false, ts);
    }

    @SafeVarargs
    public static <T> HashSet<T> createHashSet(boolean isSorted, T... ts) {
        if (null == ts) {
            return isSorted ? new LinkedHashSet<>() : new HashSet<>();
        }
        int initialCapacity = Math.max((int) (ts.length / .75f) + 1, 16);
        HashSet<T> set = isSorted ? new LinkedHashSet<>(initialCapacity) : new HashSet<>(initialCapacity);
        set.addAll(java.util.Arrays.asList(ts));

        return set;
    }

    public static <T> HashSet<T> createHashSet(Collection<T> collection) {
        return createHashSet(false, collection);
    }

    public static <T> HashSet<T> createHashSet(boolean isSorted, Collection<T> collection) {
        return isSorted ? new LinkedHashSet<>(collection) : new HashSet<>(collection);
    }

    public static <T> HashSet<T> createHashSet(boolean isSorted, Iterator<T> iter) {
        if (null == iter) {
            return createHashSet(isSorted, (T[]) null);
        }
        final HashSet<T> set = isSorted ? new LinkedHashSet<>() : new HashSet<>();
        while (iter.hasNext()) {
            set.add(iter.next());
        }
        return set;
    }

    public static <T> HashSet<T> createHashSet(boolean isSorted, Enumeration<T> enumration) {
        if (null == enumration) {
            return createHashSet(isSorted, (T[]) null);
        }
        final HashSet<T> set = isSorted ? new LinkedHashSet<>() : new HashSet<>();
        while (enumration.hasMoreElements()) {
            set.add(enumration.nextElement());
        }
        return set;
    }

    @SafeVarargs
    public static <T> ArrayList<T> createList(T... values) {
        if (null == values) {
            return new ArrayList<>();
        }
        ArrayList<T> arrayList = new ArrayList<>(values.length);
        arrayList.addAll(java.util.Arrays.asList(values));

        return arrayList;
    }

    public static <T> ArrayList<T> createList(Collection<T> collection) {
        if (null == collection) {
            return new ArrayList<>();
        }
        return new ArrayList<>(collection);
    }

    public static <T> ArrayList<T> createList(Iterable<T> iterable) {
        return (null == iterable) ? new ArrayList<>() : createList(iterable.iterator());
    }

    public static <T> ArrayList<T> createList(Iterator<T> iter) {
        final ArrayList<T> list = new ArrayList<>();
        if (null == iter) {
            return list;
        }
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    public static <T> ArrayList<T> createList(Enumeration<T> enumration) {
        final ArrayList<T> list = new ArrayList<>();
        if (null == enumration) {
            return list;
        }
        while (enumration.hasMoreElements()) {
            list.add(enumration.nextElement());
        }
        return list;
    }

    public static <T> CopyOnWriteArrayList<T> createCopyOnWriteList(Collection<T> collection) {
        return (null == collection) ? (new CopyOnWriteArrayList<>()) : (new CopyOnWriteArrayList<>(collection));
    }

    @SuppressWarnings({"unchecked", "rawtypes", "SortedCollectionWithNonComparableKeys"})
    public static <T> Collection<T> create(Class<?> collectionType) {
        Collection<T> list;
        if (collectionType.isAssignableFrom(AbstractCollection.class)) {
            list = new ArrayList<>();
        }

        // Set
        else if (collectionType.isAssignableFrom(HashSet.class)) {
            list = new HashSet<>();
        } else if (collectionType.isAssignableFrom(LinkedHashSet.class)) {
            list = new LinkedHashSet<>();
        } else if (collectionType.isAssignableFrom(TreeSet.class)) {
            list = new TreeSet<>();
        } else if (collectionType.isAssignableFrom(EnumSet.class)) {
            list = (Collection<T>) EnumSet.noneOf((Class<Enum>) ClassHelper.findTypeArgumentOfClass(collectionType));
        }

        // List
        else if (collectionType.isAssignableFrom(ArrayList.class)) {
            list = new ArrayList<>();
        } else if (collectionType.isAssignableFrom(LinkedList.class)) {
            list = new LinkedList<>();
        }

        // Others，refection
        else {
            try {
                list = (Collection<T>) ClassHelper.createInstance(collectionType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return list;
    }

    public static <K, V> Map<K, V> createMap(Class<?> mapType) {
        return Maps.create(mapType);
    }

    public static <T> ArrayList<T> distinct(Collection<T> collection) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        } else if (collection instanceof Set) {
            return new ArrayList<>(collection);
        } else {
            return new ArrayList<>(new LinkedHashSet<>(collection));
        }
    }

    public static <T> List<T> subList(List<T> list, int start, int end) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }

        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }

        final int size = list.size();
        if (end > size) {
            if (start >= size) {
                return null;
            }
            end = size;
        }

        return list.subList(start, end);
    }

    public static <T> List<T> subList(Collection<T> list, int start, int end) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        return subList(new ArrayList<>(list), start, end);
    }

    public static <T> List<List<T>> split(Collection<T> collection, int size) {
        final List<List<T>> result = new ArrayList<>();

        ArrayList<T> subList = new ArrayList<>(size);
        for (T t : collection) {
            if (subList.size() > size) {
                result.add(subList);
                subList = new ArrayList<>(size);
            }
            subList.add(t);
        }
        result.add(subList);
        return result;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return Maps.isEmpty(map);
    }

    public static boolean isEmpty(Iterable<?> iterable) {
        return Iterators.isEmpty(iterable);
    }

    public static boolean isEmpty(Iterator<?> Iterator) {
        return Iterators.isEmpty(Iterator);
    }

    public static boolean isEmpty(Enumeration<?> enumeration) {
        return null == enumeration || !enumeration.hasMoreElements();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return Maps.isNotEmpty(map);
    }

    public static boolean isNotEmpty(Iterable<?> iterable) {
        return Iterators.isNotEmpty(iterable);
    }

    public static boolean isNotEmpty(Iterator<?> Iterator) {
        return Iterators.isNotEmpty(Iterator);
    }

    public static boolean isNotEmpty(Enumeration<?> enumeration) {
        return null != enumeration && enumeration.hasMoreElements();
    }

    public static boolean containsNull(Iterable<?> iterable) {
        return Iterators.containsNull(iterable);
    }

    public static Map<String, String> zip(String keys, String values, String delimiter, boolean isOrder) {
        return Arrays.zip(Strings.split(keys, delimiter), Strings.split(values, delimiter), isOrder);
    }

    public static Map<String, String> zip(String keys, String values, String delimiter) {
        return zip(keys, values, delimiter, false);
    }

    public static <K, V> Map<K, V> zip(Collection<K> keys, Collection<V> values) {
        if (isEmpty(keys) || isEmpty(values)) {
            return null;
        }

        final List<K> keyList = new ArrayList<>(keys);
        final List<V> valueList = new ArrayList<>(values);
        final int size = Math.min(keys.size(), values.size());

        final Map<K, V> map = new HashMap<>((int) (size / 0.75));
        for (int i = 0; i < size; i++) {
            map.put(keyList.get(i), valueList.get(i));
        }

        return map;
    }

    public static <K, V> HashMap<K, V> toMap(Iterable<Map.Entry<K, V>> entryIter) {
        return Iterators.toMap(entryIter);
    }

    public static HashMap<Object, Object> toMap(Object[] array) {
        return Maps.of(array);
    }

    public static <T> TreeSet<T> toTreeSet(Collection<T> collection, Comparator<T> comparator) {
        final TreeSet<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);

        return treeSet;
    }

    public static <E> Iterator<E> toIterator(Enumeration<E> e) {
        return Iterators.create(e);
    }

    public static <E> Iterable<E> toIterable(final Iterator<E> iter) {
        return Iterators.createIterable(iter);
    }

    public static <E> Collection<E> toCollection(Iterable<E> iterable) {
        return (iterable instanceof Collection) ? (Collection<E>) iterable : createList(iterable.iterator());
    }

    public static <T> Collection<T> addAll(Collection<T> collection, Iterator<T> iterator) {
        if (null != collection && null != iterator) {
            while (iterator.hasNext()) {
                collection.add(iterator.next());
            }
        }
        return collection;
    }

    public static <T> Collection<T> addAll(Collection<T> collection, Iterable<T> iterable) {
        return addAll(collection, iterable.iterator());
    }

    public static <T> Collection<T> addAll(Collection<T> collection, Enumeration<T> enumeration) {
        if (null != collection && null != enumeration) {
            while (enumeration.hasMoreElements()) {
                collection.add(enumeration.nextElement());
            }
        }
        return collection;
    }

    public static <T> Collection<T> addAll(Collection<T> collection, T[] values) {
        if (null != collection && null != values) {
            Collections.addAll(collection, values);
        }
        return collection;
    }

    public static <T> List<T> addAllIfNotContains(List<T> list, List<T> otherList) {
        for (T t : otherList) {
            if (!list.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }

    public static <T> T getFirst(Iterable<T> iterable) {
        return Iterators.getFirst(iterable);
    }

    public static <T> T getFirst(Iterator<T> iterator) {
        return Iterators.getFirst(iterator);
    }

    public static Class<?> getElementType(Iterable<?> iterable) {
        return Iterators.getElementType(iterable);
    }

    public static Class<?> getElementType(Iterator<?> iterator) {
        return Iterators.getElementType(iterator);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ArrayList<V> valuesOfKeys(Map<K, V> map, K... keys) {
        final ArrayList<V> values = new ArrayList<>();
        for (K k : keys) {
            values.add(map.get(k));
        }
        return values;
    }

    public static <K, V> ArrayList<V> valuesOfKeys(Map<K, V> map, Iterable<K> keys) {
        return valuesOfKeys(map, keys.iterator());
    }

    public static <K, V> ArrayList<V> valuesOfKeys(Map<K, V> map, Iterator<K> keys) {
        final ArrayList<V> values = new ArrayList<>();
        while (keys.hasNext()) {
            values.add(map.get(keys.next()));
        }
        return values;
    }

    public static <T> List<T> sort(Collection<T> collection, Comparator<? super T> comparator) {
        List<T> list = new ArrayList<>(collection);
        list.sort(comparator);
        return list;
    }

    public static <T> List<T> sort(List<T> list, Comparator<? super T> comparator) {
        List<T> listCopy = new ArrayList<>(list);
        listCopy.sort(comparator);
        return listCopy;
    }

    public static <K, V> TreeMap<K, V> sort(Map<K, V> map, Comparator<? super K> comparator) {
        final TreeMap<K, V> result = new TreeMap<>(comparator);
        result.putAll(map);
        return result;
    }

    public static <K, V> LinkedHashMap<K, V> sortMap(Collection<Map.Entry<K, V>> entryCollection, Comparator<Map.Entry<K, V>> comparator) {
        List<Map.Entry<K, V>> list = new LinkedList<>(entryCollection);
        list.sort(comparator);

        LinkedHashMap<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V> LinkedHashMap<K, V> sortMap(Map<K, V> map, Comparator<Map.Entry<K, V>> comparator) {
        return sortMap(map.entrySet(), comparator);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> List<Map.Entry<K, V>> sortMap(Collection<Map.Entry<K, V>> collection) {
        List<Map.Entry<K, V>> list = new LinkedList<>(collection);
        list.sort((o1, o2) -> {
            V v1 = o1.getValue();
            V v2 = o2.getValue();

            if (v1 instanceof Comparable) {
                return ((Comparable<V>) v1).compareTo(v2);
            } else {
                return v1.toString().compareTo(v2.toString());
            }
        });

        return list;
    }

    public static <T> void forEach(Iterator<T> iterator, IdxConsumer<T> consumer) {
        int index = 0;
        while (iterator.hasNext()) {
            consumer.accept(iterator.next(), index);
            index++;
        }
    }

    public static <T> void forEach(Enumeration<T> enumeration, IdxConsumer<T> consumer) {
        int index = 0;
        while (enumeration.hasMoreElements()) {
            consumer.accept(enumeration.nextElement(), index);
            index++;
        }
    }

    public static <K, V> void forEach(Map<K, V> map, KVConsumer<K, V> kvConsumer) {
        int index = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            kvConsumer.accept(entry.getKey(), entry.getValue(), index);
            index++;
        }
    }


    public interface IdxConsumer<T> {
        void accept(T value, int index);
    }

    public interface KVConsumer<K, V> {
        void accept(K key, V value, int index);
    }
}
