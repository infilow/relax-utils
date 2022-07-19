package com.infilos.utils;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.*;

@SuppressWarnings({"UnnecessaryBoxing", "UnnecessaryUnboxing", "unused"})
public final class Arrays {
    private Arrays() {
    }

    private static final int INDEX_NOT_FOUND = -1;

    @SuppressWarnings("unchecked")
    public static <T> boolean isEmpty(final T... array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(final Object array) {
        if (null == array) {
            return true;
        } else if (!isArray(array)) {
            return false;
        } else {
            return 0 == Array.getLength(array);
        }
    }

    public static boolean isEmpty(final long... array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(final int... array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(final short... array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(final char... array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(final byte... array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(final double... array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(final float... array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(final boolean... array) {
        return array == null || array.length == 0;
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean isNotEmpty(final T... array) {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(final Object array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(final long... array) {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(final int... array) {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(final short... array) {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(final char... array) {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(final byte... array) {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(final double... array) {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(final float... array) {
        return (array != null && array.length != 0);
    }

    public static boolean isNotEmpty(final boolean... array) {
        return (array != null && array.length != 0);
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean containsNull(T... array) {
        if (isNotEmpty(array)) {
            for (T element : array) {
                if (null == element) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] create(Class<?> componentType, int newSize) {
        return (T[]) Array.newInstance(componentType, newSize);
    }

    /**
     * The array element must can be cast, return a new array.
     */
    public static Object[] cast(Class<?> type, Object arrayObject) throws NullPointerException, IllegalArgumentException {
        if (null == arrayObject) {
            throw new NullPointerException("Argument [arrayObject] is null !");
        }
        if (!arrayObject.getClass().isArray()) {
            throw new IllegalArgumentException("Argument [arrayObject] is not array !");
        }
        if (null == type) {
            return (Object[]) arrayObject;
        }

        final Class<?> componentType = type.isArray() ? type.getComponentType() : type;
        final Object[] array = (Object[]) arrayObject;
        final Object[] result = create(componentType, array.length);
        System.arraycopy(array, 0, result, 0, array.length);

        return result;
    }

    @SafeVarargs
    public static <T> T[] append(T[] buffer, T... newElements) {
        if (isEmpty(newElements)) {
            return buffer;
        }

        T[] t = resize(buffer, buffer.length + newElements.length);
        System.arraycopy(newElements, 0, t, buffer.length, newElements.length);
        return t;
    }

    public static <T> T[] resize(T[] buffer, int newSize, Class<?> componentType) {
        T[] newArray = create(componentType, newSize);
        if (isNotEmpty(buffer)) {
            System.arraycopy(buffer, 0, newArray, 0, Math.min(buffer.length, newSize));
        }
        return newArray;
    }

    public static <T> T[] resize(T[] buffer, int newSize) {
        return resize(buffer, newSize, buffer.getClass().getComponentType());
    }

    @SafeVarargs
    public static <T> T[] addAll(T[]... arrays) {
        if (arrays.length == 1) {
            return arrays[0];
        }

        int length = 0;
        for (T[] array : arrays) {
            if (array == null) {
                continue;
            }
            length += array.length;
        }
        T[] result = create(arrays.getClass().getComponentType().getComponentType(), length);

        length = 0;
        for (T[] array : arrays) {
            if (array == null) {
                continue;
            }
            System.arraycopy(array, 0, result, length, array.length);
            length += array.length;
        }
        return result;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static Object copy(Object src, int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
        return dest;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static Object copy(Object src, Object dest, int length) {
        System.arraycopy(src, 0, dest, 0, length);
        return dest;
    }

    public static <T> T[] clone(T[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    @SuppressWarnings("unchecked")
    public static <T> T clone(final T obj) {
        if (null == obj) {
            return null;
        }
        if (isArray(obj)) {
            final Object result;
            final Class<?> componentType = obj.getClass().getComponentType();
            if (componentType.isPrimitive()) {
                int length = Array.getLength(obj);
                result = Array.newInstance(componentType, length);
                while (length-- > 0) {
                    Array.set(result, length, Array.get(obj, length));
                }
            } else {
                result = ((Object[]) obj).clone();
            }
            return (T) result;
        }
        return null;
    }

    public static int[] range(int excludedEnd) {
        return range(0, excludedEnd, 1);
    }

    public static int[] range(int includedStart, int excludedEnd) {
        return range(includedStart, excludedEnd, 1);
    }

    public static int[] range(int includedStart, int excludedEnd, int step) {
        if (includedStart > excludedEnd) {
            int tmp = includedStart;
            includedStart = excludedEnd;
            excludedEnd = tmp;
        }

        if (step <= 0) {
            step = 1;
        }

        int deviation = excludedEnd - includedStart;
        int length = deviation / step;
        if (deviation % step != 0) {
            length += 1;
        }
        int[] range = new int[length];
        for (int i = 0; i < length; i++) {
            range[i] = includedStart;
            includedStart += step;
        }
        return range;
    }

    public static byte[][] split(byte[] array, int len) {
        int x = array.length / len;
        int y = array.length % len;
        int z = 0;
        if (y != 0) {
            z = 1;
        }
        byte[][] arrays = new byte[x + z][];
        byte[] arr;
        for (int i = 0; i < x + z; i++) {
            arr = new byte[len];
            if (i == x + z - 1 && y != 0) {
                System.arraycopy(array, i * len, arr, 0, y);
            } else {
                System.arraycopy(array, i * len, arr, 0, len);
            }
            arrays[i] = arr;
        }
        return arrays;
    }

    public static <K, V> Map<K, V> zip(K[] keys, V[] values, boolean isOrder) {
        if (isEmpty(keys) || isEmpty(values)) {
            return null;
        }

        final int size = Math.min(keys.length, values.length);
        final Map<K, V> map = Maps.create(size, isOrder);
        for (int i = 0; i < size; i++) {
            map.put(keys[i], values[i]);
        }

        return map;
    }

    public static <K, V> Map<K, V> zip(K[] keys, V[] values) {
        return zip(keys, values, false);
    }

    public static <T> int indexOf(T[] array, Object value) {
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(value, array[i])) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static <T> int lastIndexOf(T[] array, Object value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (Objects.equals(value, array[i])) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static <T> boolean contains(T[] array, T value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static int indexOf(long[] array, long value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(long[] array, long value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean contains(long[] array, long value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(int[] array, int value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean contains(int[] array, int value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static int indexOf(short[] array, short value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(short[] array, short value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean contains(short[] array, short value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static int indexOf(char[] array, char value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(char[] array, char value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean contains(char[] array, char value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static int indexOf(byte[] array, byte value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(byte[] array, byte value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean contains(byte[] array, byte value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static int indexOf(double[] array, double value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(double[] array, double value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean contains(double[] array, double value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static int indexOf(float[] array, float value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(float[] array, float value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean contains(float[] array, float value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static int indexOf(boolean[] array, boolean value) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(boolean[] array, boolean value) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean contains(boolean[] array, boolean value) {
        return indexOf(array, value) > INDEX_NOT_FOUND;
    }

    public static Integer[] wrap(int... values) {
        final int length = values.length;
        Integer[] array = new Integer[length];
        for (int i = 0; i < length; i++) {
            array[i] = Integer.valueOf(values[i]);
        }
        return array;
    }

    public static int[] unwrap(Integer... values) {
        final int length = values.length;
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = values[i].intValue();
        }
        return array;
    }

    public static Long[] wrap(long... values) {
        final int length = values.length;
        Long[] array = new Long[length];
        for (int i = 0; i < length; i++) {
            array[i] = Long.valueOf(values[i]);
        }
        return array;
    }

    public static long[] unwrap(Long... values) {
        final int length = values.length;
        long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = values[i].longValue();
        }
        return array;
    }

    public static Character[] wrap(char... values) {
        final int length = values.length;
        Character[] array = new Character[length];
        for (int i = 0; i < length; i++) {
            array[i] = Character.valueOf(values[i]);
        }
        return array;
    }

    public static char[] unwrap(Character... values) {
        final int length = values.length;
        char[] array = new char[length];
        for (int i = 0; i < length; i++) {
            array[i] = values[i].charValue();
        }
        return array;
    }

    public static Byte[] wrap(byte... values) {
        final int length = values.length;
        Byte[] array = new Byte[length];
        for (int i = 0; i < length; i++) {
            array[i] = Byte.valueOf(values[i]);
        }
        return array;
    }

    public static byte[] unwrap(Byte... values) {
        final int length = values.length;
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++) {
            array[i] = values[i].byteValue();
        }
        return array;
    }

    public static Short[] wrap(short... values) {
        final int length = values.length;
        Short[] array = new Short[length];
        for (int i = 0; i < length; i++) {
            array[i] = Short.valueOf(values[i]);
        }
        return array;
    }

    public static short[] unwrap(Short... values) {
        final int length = values.length;
        short[] array = new short[length];
        for (int i = 0; i < length; i++) {
            array[i] = values[i].shortValue();
        }
        return array;
    }

    public static Float[] wrap(float... values) {
        final int length = values.length;
        Float[] array = new Float[length];
        for (int i = 0; i < length; i++) {
            array[i] = Float.valueOf(values[i]);
        }
        return array;
    }

    public static float[] unwrap(Float... values) {
        final int length = values.length;
        float[] array = new float[length];
        for (int i = 0; i < length; i++) {
            array[i] = values[i].floatValue();
        }
        return array;
    }

    public static Double[] wrap(double... values) {
        final int length = values.length;
        Double[] array = new Double[length];
        for (int i = 0; i < length; i++) {
            array[i] = Double.valueOf(values[i]);
        }
        return array;
    }

    public static double[] unwrap(Double... values) {
        final int length = values.length;
        double[] array = new double[length];
        for (int i = 0; i < length; i++) {
            array[i] = values[i].doubleValue();
        }
        return array;
    }

    public static Boolean[] wrap(boolean... values) {
        final int length = values.length;
        Boolean[] array = new Boolean[length];
        for (int i = 0; i < length; i++) {
            array[i] = Boolean.valueOf(values[i]);
        }
        return array;
    }

    public static boolean[] unwrap(Boolean... values) {
        final int length = values.length;
        boolean[] array = new boolean[length];
        for (int i = 0; i < length; i++) {
            array[i] = values[i].booleanValue();
        }
        return array;
    }

    public static Object[] wrap(Object obj) {
        if (isArray(obj)) {
            try {
                return (Object[]) obj;
            } catch (Exception e) {
                final String className = obj.getClass().getComponentType().getName();
                switch (className) {
                    case "long":
                        return wrap((long[]) obj);
                    case "int":
                        return wrap((int[]) obj);
                    case "short":
                        return wrap((short[]) obj);
                    case "char":
                        return wrap((char[]) obj);
                    case "byte":
                        return wrap((byte[]) obj);
                    case "boolean":
                        return wrap((boolean[]) obj);
                    case "float":
                        return wrap((float[]) obj);
                    case "double":
                        return wrap((double[]) obj);
                    default:
                        throw new RuntimeException(e);
                }
            }
        }
        throw Throws.runtime("[%s] is not Array!", obj.getClass());
    }

    public static boolean isArray(Object obj) {
        return Require.checkNotNull(obj).getClass().isArray();
    }

    @SuppressWarnings("ConstantConditions")
    public static String toString(Object obj) {
        if (null == obj) {
            return null;
        }
        if (Arrays.isArray(obj)) {
            try {
                return java.util.Arrays.deepToString((Object[]) obj);
            } catch (Exception e) {
                final String className = obj.getClass().getComponentType().getName();
                switch (className) {
                    case "long":
                        return java.util.Arrays.toString((long[]) obj);
                    case "int":
                        return java.util.Arrays.toString((int[]) obj);
                    case "short":
                        return java.util.Arrays.toString((short[]) obj);
                    case "char":
                        return java.util.Arrays.toString((char[]) obj);
                    case "byte":
                        return java.util.Arrays.toString((byte[]) obj);
                    case "boolean":
                        return java.util.Arrays.toString((boolean[]) obj);
                    case "float":
                        return java.util.Arrays.toString((float[]) obj);
                    case "double":
                        return java.util.Arrays.toString((double[]) obj);
                    default:
                        throw new RuntimeException(e);
                }
            }
        }
        return obj.toString();
    }

    public static int length(Object array) throws IllegalArgumentException {
        if (null == array) {
            return 0;
        }
        return Array.getLength(array);
    }

    public static byte[] toArray(ByteBuffer bytebuffer) {
        if (!bytebuffer.hasArray()) {
            int oldPosition = bytebuffer.position();
            bytebuffer.position(0);
            int size = bytebuffer.limit();
            byte[] buffers = new byte[size];
            bytebuffer.get(buffers);
            bytebuffer.position(oldPosition);
            return buffers;
        } else {
            return java.util.Arrays.copyOfRange(bytebuffer.array(), bytebuffer.position(), bytebuffer.limit());
        }
    }

    public static <T> T[] toArray(Iterator<T> iterator, Class<T> componentType) {
        return toArray(Collects.createList(iterator), componentType);
    }

    public static <T> T[] toArray(Iterable<T> iterable, Class<T> componentType) {
        return toArray(Collects.toCollection(iterable), componentType);
    }

    public static <T> T[] toArray(Collection<T> collection, Class<T> componentType) {
        final T[] array = create(componentType, collection.size());
        return collection.toArray(array);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] remove(T[] array, int index) throws IllegalArgumentException {
        return (T[]) remove((Object) array, index);
    }

    public static long[] remove(long[] array, int index) throws IllegalArgumentException {
        return (long[]) remove((Object) array, index);
    }

    public static int[] remove(int[] array, int index) throws IllegalArgumentException {
        return (int[]) remove((Object) array, index);
    }

    public static short[] remove(short[] array, int index) throws IllegalArgumentException {
        return (short[]) remove((Object) array, index);
    }

    public static char[] remove(char[] array, int index) throws IllegalArgumentException {
        return (char[]) remove((Object) array, index);
    }

    public static byte[] remove(byte[] array, int index) throws IllegalArgumentException {
        return (byte[]) remove((Object) array, index);
    }

    public static double[] remove(double[] array, int index) throws IllegalArgumentException {
        return (double[]) remove((Object) array, index);
    }

    public static float[] remove(float[] array, int index) throws IllegalArgumentException {
        return (float[]) remove((Object) array, index);
    }

    public static boolean[] remove(boolean[] array, int index) throws IllegalArgumentException {
        return (boolean[]) remove((Object) array, index);
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static Object remove(Object array, int index) throws IllegalArgumentException {
        if (null == array) {
            return null;
        }
        int length = length(array);
        if (index < 0 || index >= length) {
            return array;
        }

        final Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < length - 1) {
            System.arraycopy(array, index + 1, result, index, length - index - 1);
        }

        return result;
    }

    public static <T> T[] removeFirst(T[] array, T element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static long[] removeFirst(long[] array, long element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static int[] removeFirst(int[] array, int element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static short[] removeFirst(short[] array, short element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static char[] removeFirst(char[] array, char element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static byte[] removeFirst(byte[] array, byte element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static double[] removeFirst(double[] array, double element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static float[] removeFirst(float[] array, float element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static boolean[] removeFirst(boolean[] array, boolean element) throws IllegalArgumentException {
        return remove(array, indexOf(array, element));
    }

    public static <T> T[] reverse(final T[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        T tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static <T> T[] reverse(final T[] array) {
        return reverse(array, 0, array.length);
    }

    public static long[] reverse(final long[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        long tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static long[] reverse(final long[] array) {
        return reverse(array, 0, array.length);
    }

    public static int[] reverse(final int[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        int tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static int[] reverse(final int[] array) {
        return reverse(array, 0, array.length);
    }

    public static short[] reverse(final short[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        short tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static short[] reverse(final short[] array) {
        return reverse(array, 0, array.length);
    }

    public static char[] reverse(final char[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        char tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static char[] reverse(final char[] array) {
        return reverse(array, 0, array.length);
    }

    public static byte[] reverse(final byte[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static byte[] reverse(final byte[] array) {
        return reverse(array, 0, array.length);
    }

    public static double[] reverse(final double[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        double tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static double[] reverse(final double[] array) {
        return reverse(array, 0, array.length);
    }

    public static float[] reverse(final float[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        float tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static float[] reverse(final float[] array) {
        return reverse(array, 0, array.length);
    }

    public static boolean[] reverse(final boolean[] array, final int startIndexInclusive, final int endIndexExclusive) {
        if (isEmpty(array)) {
            return array;
        }
        int i = Math.max(startIndexInclusive, 0);
        int j = Math.min(array.length, endIndexExclusive) - 1;
        boolean tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
        return array;
    }

    public static boolean[] reverse(final boolean[] array) {
        return reverse(array, 0, array.length);
    }

    public static <T extends Comparable<? super T>> T min(T[] numberArray) {
        T min = numberArray[0];
        for (T t : numberArray) {
            if (Compares.compare(min, t) > 0) {
                min = t;
            }
        }
        return min;
    }

    public static long min(long[] numberArray) {
        long min = numberArray[0];
        for (long l : numberArray) {
            if (min > l) {
                min = l;
            }
        }
        return min;
    }

    public static int min(int[] numberArray) {
        int min = numberArray[0];
        for (int j : numberArray) {
            if (min > j) {
                min = j;
            }
        }
        return min;
    }

    public static short min(short[] numberArray) {
        short min = numberArray[0];
        for (short value : numberArray) {
            if (min > value) {
                min = value;
            }
        }
        return min;
    }

    public static char min(char[] numberArray) {
        char min = numberArray[0];
        for (char c : numberArray) {
            if (min > c) {
                min = c;
            }
        }
        return min;
    }

    public static byte min(byte[] numberArray) {
        byte min = numberArray[0];
        for (byte b : numberArray) {
            if (min > b) {
                min = b;
            }
        }
        return min;
    }

    public static double min(double[] numberArray) {
        double min = numberArray[0];
        for (double v : numberArray) {
            if (min > v) {
                min = v;
            }
        }
        return min;
    }

    public static float min(float[] numberArray) {
        float min = numberArray[0];
        for (float v : numberArray) {
            if (min > v) {
                min = v;
            }
        }
        return min;
    }

    public static <T extends Comparable<? super T>> T max(T[] numberArray) {
        T max = numberArray[0];
        for (T t : numberArray) {
            if (Compares.compare(max, t) < 0) {
                max = t;
            }
        }
        return max;
    }

    public static long max(long[] numberArray) {
        long max = numberArray[0];
        for (long l : numberArray) {
            if (max < l) {
                max = l;
            }
        }
        return max;
    }

    public static int max(int[] numberArray) {
        int max = numberArray[0];
        for (int j : numberArray) {
            if (max < j) {
                max = j;
            }
        }
        return max;
    }

    public static short max(short[] numberArray) {
        short max = numberArray[0];
        for (short value : numberArray) {
            if (max < value) {
                max = value;
            }
        }
        return max;
    }

    public static char max(char[] numberArray) {
        char max = numberArray[0];
        for (char c : numberArray) {
            if (max < c) {
                max = c;
            }
        }
        return max;
    }

    public static byte max(byte[] numberArray) {
        byte max = numberArray[0];
        for (byte b : numberArray) {
            if (max < b) {
                max = b;
            }
        }
        return max;
    }

    public static double max(double[] numberArray) {
        double max = numberArray[0];
        for (double v : numberArray) {
            if (max < v) {
                max = v;
            }
        }
        return max;
    }

    public static float max(float[] numberArray) {
        float max = numberArray[0];
        for (float v : numberArray) {
            if (max < v) {
                max = v;
            }
        }
        return max;
    }
}
