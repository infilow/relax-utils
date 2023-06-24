package com.infilos.reflect;

import com.infilos.utils.Arrays;

import java.beans.PropertyEditor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URL;
import java.util.*;

public final class TypeHelper {
    private TypeHelper() {
    }

    static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<Class<?>, Class<?>>() {{
        put(boolean.class, Boolean.class);
        put(byte.class, Byte.class);
        put(char.class, Character.class);
        put(double.class, Double.class);
        put(float.class, Float.class);
        put(int.class, Integer.class);
        put(long.class, Long.class);
        put(short.class, Short.class);
    }};
    static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVES = new HashMap<Class<?>, Class<?>>() {{
        put(Boolean.class, boolean.class);
        put(Byte.class, byte.class);
        put(Character.class, char.class);
        put(Double.class, double.class);
        put(Float.class, float.class);
        put(Integer.class, int.class);
        put(Long.class, long.class);
        put(Short.class, short.class);
    }};

    public static Class<?> boxType(final Class<?> type) {
        return type.isPrimitive() ? PRIMITIVE_WRAPPERS.get(type) : type;
    }

    public static Class<?> unboxType(Class<?> clazz) {
        if (null == clazz || !clazz.isPrimitive()) {
            return clazz;
        }
        Class<?> result = WRAPPER_PRIMITIVES.get(clazz);

        return (null == result) ? clazz : result;
    }

    public static boolean isPrimitive(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return PRIMITIVE_WRAPPERS.containsKey(clazz);
    }

    public static boolean isBasicType(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
    }

    public static boolean isPrimitiveWrapper(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return WRAPPER_PRIMITIVES.containsKey(clazz);
    }

    public static boolean isSimpleTypeOrArray(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return isSimpleValueType(clazz) || (clazz.isArray() && isSimpleValueType(clazz.getComponentType()));
    }

    public static boolean isSimpleValueType(Class<?> clazz) {
        return isBasicType(clazz)
                || clazz.isEnum()
                || CharSequence.class.isAssignableFrom(clazz)
                || Number.class.isAssignableFrom(clazz)
                || Date.class.isAssignableFrom(clazz)
                || clazz.equals(URI.class)
                || clazz.equals(URL.class)
                || clazz.equals(Locale.class)
                || clazz.equals(Class.class);
    }

    public static boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
        if (null == targetType || null == sourceType) {
            return false;
        }

        // object type
        if (targetType.isAssignableFrom(sourceType)) {
            return true;
        }

        // basic type
        if (targetType.isPrimitive()) {
            // primitive
            Class<?> resolvedPrimitive = WRAPPER_PRIMITIVES.get(sourceType);
            return targetType.equals(resolvedPrimitive);
        } else {
            // wrapper
            Class<?> resolvedWrapper = PRIMITIVE_WRAPPERS.get(sourceType);
            return resolvedWrapper != null && targetType.isAssignableFrom(resolvedWrapper);
        }
    }

    public static boolean isAllAssignable(Class<?>[] targetTypes, Class<?>[] sourceTypes) {
        if (com.infilos.utils.Arrays.isEmpty(targetTypes) && Arrays.isEmpty(sourceTypes)) {
            return true;
        }
        if (targetTypes.length == sourceTypes.length) {
            for (int i = 0; i < targetTypes.length; i++) {
                if (!targetTypes[i].isAssignableFrom(sourceTypes[i])) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    public static boolean isPublic(Class<?> clazz) {
        if (null == clazz) {
            throw new NullPointerException("Class to check is null.");
        }
        return Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isPublic(Method method) {
        if (null == method) {
            throw new NullPointerException("Method to check is null.");
        }
        return isPublic(method.getDeclaringClass());
    }

    public static boolean isNotPublic(Class<?> clazz) {
        return !isPublic(clazz);
    }

    public static boolean isNotPublic(Method method) {
        return !isPublic(method);
    }

    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static Method setAccessible(Method method) {
        if (null != method && !method.isAccessible()) {
            method.setAccessible(true);
        }
        return method;
    }

    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    public static boolean isNormalClass(Class<?> clazz) {
        return null != clazz
                && !clazz.isInterface()
                && !isAbstract(clazz)
                && !clazz.isEnum()
                && !clazz.isArray()
                && !clazz.isAnnotation()
                && !clazz.isSynthetic()
                && !clazz.isPrimitive();
    }

    public static Object getDefaultValue(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (long.class == clazz) {
                return 0L;
            } else if (int.class == clazz) {
                return 0;
            } else if (short.class == clazz) {
                return (short) 0;
            } else if (char.class == clazz) {
                return (char) 0;
            } else if (byte.class == clazz) {
                return (byte) 0;
            } else if (double.class == clazz) {
                return 0D;
            } else if (float.class == clazz) {
                return 0f;
            } else if (boolean.class == clazz) {
                return false;
            }
        }

        return null;
    }

    public static Object[] getDefaultValues(Class<?>... classes) {
        final Object[] values = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            values[i] = getDefaultValue(classes[i]);
        }

        return values;
    }

    /**
     * Casts object to type if it's a non-null instance of T, or else returns Optional.empty().
     */
    public static <T> Optional<T> cast(Object object, Class<T> type) {
        return type.isInstance(object) ? Optional.of(type.cast(object)) : Optional.empty();
    }

    /**
     * Cast string value to the instance of target type.
     */
    public static Object castString(final String value, final Type targetType, final String name) {
        if (targetType instanceof Class) {
            return castObject(value, (Class<?>) targetType, name);
        }

        if (targetType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) targetType;
            final Type raw = parameterizedType.getRawType();
            if (!(raw instanceof Class)) {
                throw new IllegalArgumentException("not supported parameterized type: " + targetType);
            }

            final Class<?> rawClass = (Class<?>) raw;
            final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (Collection.class.isAssignableFrom(rawClass)) {
                final Class<?> argType = actualTypeArguments.length == 0 ? String.class : castType(actualTypeArguments[0]);
                final String[] split = value.split(" *, *");

                final Collection<Object> values;
                if (Collection.class == raw || List.class == raw) {
                    values = new ArrayList<>(split.length);
                } else if (Set.class == raw) {
                    values = SortedSet.class.isAssignableFrom(rawClass) ? new TreeSet<>() : new HashSet<>(split.length);
                } else {
                    throw new IllegalArgumentException(targetType + " collection type not supported");
                }

                for (final String val : split) {
                    values.add(castObject(val, argType, name));
                }

                return values;
            } else if (Map.class.isAssignableFrom(rawClass)) {
                final Map<Object, Object> map;
                if (SortedMap.class == raw) {
                    map = new TreeMap<>();
                } else {
                    map = new HashMap<>();
                }
                final Properties p = new Properties();
                try {
                    p.load(new ByteArrayInputStream(value.getBytes()));
                } catch (final IOException e) {
                    // can't occur
                }
                final Class<?> keyType = actualTypeArguments.length == 0 ? String.class : castType(actualTypeArguments[0]);
                final Class<?> valueType = actualTypeArguments.length == 0 ? String.class : castType(actualTypeArguments[1]);
                for (final String k : p.stringPropertyNames()) {
                    map.put(castObject(k, keyType, name), castObject(p.getProperty(k), valueType, name));
                }
                return map;
            }
        }
        throw new IllegalArgumentException("not supported type: " + targetType);
    }

    private static Class<?> castType(final Type type) {
        try {
            return (Class<?>) type;
        } catch (final Exception e) {
            throw new IllegalArgumentException(type + " not supported");
        }
    }

    /**
     * Convert object value to instance of target type.
     */
    public static Object castObject(final Object value, Class<?> targetType, final String name) {
        if (value == null) {
            if (targetType.equals(Boolean.TYPE)) {
                return false;
            }

            return null;
        }

        final Class<?> actualType = value.getClass();

        if (targetType.isPrimitive()) {
            targetType = boxType(targetType);
        }
        if (targetType.isAssignableFrom(actualType)) {
            return value;
        }
        if (Number.class.isAssignableFrom(actualType) && Number.class.isAssignableFrom(targetType)) {
            return value;
        }
        if (!(value instanceof String)) {
            final String message = String.format("Expected type '%s' for '%s'. Found '%s'", targetType.getName(), name, actualType.getName());
            throw new IllegalArgumentException(message);
        }

        final String stringValue = (String) value;

        try {
            // Force static initializers to run
            Class.forName(targetType.getName(), true, targetType.getClassLoader());
        } catch (final ClassNotFoundException e) {
            // no-op
        }

        final Optional<PropertyEditor> editor = PropertyEditors.of(targetType);

        if (!editor.isPresent()) {
            final Object result = create(targetType, stringValue);

            if (result != null) {
                return result;
            }
        }

        if (!editor.isPresent()) {
            final String message = String.format("Cannot convert to '%s' for '%s'. No PropertyEditor", targetType.getName(), name);
            throw new IllegalArgumentException(message);
        }

        editor.get().setAsText(stringValue);

        return editor.get().getValue();
    }

    /**
     * Create instance of target type from string.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object create(final Class<?> type, final String value) {

        if (Enum.class.isAssignableFrom(type)) {
            final Class<? extends Enum> enumType = (Class<? extends Enum>) type;
            try {
                return Enum.valueOf(enumType, value);
            } catch (final IllegalArgumentException e) {
                try {
                    return Enum.valueOf(enumType, value.toUpperCase());
                } catch (final IllegalArgumentException e1) {
                    return Enum.valueOf(enumType, value.toLowerCase());
                }
            }
        }

        try {
            final Constructor<?> constructor = type.getConstructor(String.class);
            return constructor.newInstance(value);
        } catch (final NoSuchMethodException e) {
            // fine
        } catch (final Exception e) {
            final String message = String.format("Cannot convert string '%s' to %s.", value, type);
            throw new IllegalArgumentException(message, e);
        }

        for (final Method method : type.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (!method.getReturnType().equals(type)) {
                continue;
            }
            if (method.getParameterTypes().length != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].equals(String.class)) {
                continue;
            }

            try {
                return method.invoke(null, value);
            } catch (final Exception e) {
                final String message = String.format("Cannot convert string '%s' to %s.", value, type);
                throw new IllegalStateException(message, e);
            }
        }

        return null;
    }
}
