package com.infilos.reflect;

import com.infilos.utils.Require;
import com.infilos.utils.Strings;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassLoaders {
    private ClassLoaders() {
    }

    private static final String ARRAY_SUFFIX = "[]";
    private static final String INTERNAL_ARRAY_PREFIX = "[";
    private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";
    private static final char PACKAGE_SEPARATOR = '.';
    private static final char INNER_CLASS_SEPARATOR = '$';
    private static final Map<String, Class<?>> PRIMITIVE_NAME_TYPE = new ConcurrentHashMap<>(32);

    static {
        List<Class<?>> primitiveTypes = new ArrayList<Class<?>>(32){{
            addAll(TypeHelper.PRIMITIVE_WRAPPERS.keySet());
            add(boolean[].class);
            add(byte[].class);
            add(char[].class);
            add(double[].class);
            add(float[].class);
            add(int[].class);
            add(long[].class);
            add(short[].class);
            add(void.class);    
        }};
        
        for (Class<?> primitiveType : primitiveTypes) {
            PRIMITIVE_NAME_TYPE.put(primitiveType.getName(), primitiveType);
        }
    }

    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static ClassLoader getClassLoader() {
        ClassLoader classLoader = getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoaders.class.getClassLoader();
            if (null == classLoader) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return classLoader;
    }

    public static Class<?> loadClass(String name) {
        return loadClass(name, true);
    }

    public static Class<?> loadClass(String name, boolean isInitialized) {
        return loadClass(name, null, isInitialized);
    }

    public static Class<?> loadClass(String name, ClassLoader classLoader, boolean isInitialized) {
        Require.checkNotNull(name, "Name must not be null");

        Class<?> clazz = loadPrimitiveClass(name);
        if (clazz != null) {
            return clazz;
        }

        // java.lang.String[]
        if (name.endsWith(ARRAY_SUFFIX)) {
            final String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            final Class<?> elementClass = loadClass(elementClassName, classLoader, isInitialized);
            clazz = Array.newInstance(elementClass, 0).getClass();
        }
        // [Ljava.lang.String;
        else if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            final String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            final Class<?> elementClass = loadClass(elementName, classLoader, isInitialized);
            clazz = Array.newInstance(elementClass, 0).getClass();
        }
        // [[I or [[Ljava.lang.String;
        else if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            final String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
            final Class<?> elementClass = loadClass(elementName, classLoader, isInitialized);
            clazz = Array.newInstance(elementClass, 0).getClass();
        }
        // common class
        else {
            if (null == classLoader) {
                classLoader = getClassLoader();
            }
            try {
                clazz = Class.forName(name, isInitialized, classLoader);
            } catch (ClassNotFoundException ex) {
                // internal class
                int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
                if (lastDotIndex > 0) {
                    final String innerClassName = name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
                    try {
                        clazz = Class.forName(innerClassName, isInitialized, classLoader);
                    } catch (ClassNotFoundException ex2) {
                    }
                }
                throw new RuntimeException(ex);
            }
        }

        return clazz;
    }

    public static Class<?> loadPrimitiveClass(String name) {
        Class<?> result = null;
        if (Strings.isNotBlank(name)) {
            name = name.trim();
            if (name.length() <= 8) {
                result = PRIMITIVE_NAME_TYPE.get(name);
            }
        }
        return result;
    }

    public static boolean isPresent(String className) {
        return isPresent(className, null);
    }

    public static boolean isPresent(String className, ClassLoader classLoader) {
        try {
            loadClass(className, classLoader, false);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
