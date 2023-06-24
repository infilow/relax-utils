package com.infilos.reflect;

import com.infilos.utils.*;
import com.infilos.utils.Arrays;

import java.lang.reflect.*;
import java.util.*;

public final class ClassHelper {
    private ClassHelper() {
    }

    static final Map<String, Class<?>> PRIMITIVES = new HashMap<String, Class<?>>() {{
        put("boolean", boolean.class);
        put("byte", byte.class);
        put("char", char.class);
        put("short", short.class);
        put("int", int.class);
        put("long", long.class);
        put("float", float.class);
        put("double", double.class);
    }};


    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(T obj) {
        return ((null == obj) ? null : (Class<T>) obj.getClass());
    }

    public static Class<?>[] getClasses(Object... objects) {
        Class<?>[] classes = new Class<?>[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            classes[i] = (null == obj) ? Object.class : obj.getClass();
        }
        return classes;
    }

    public static boolean isEquals(Class<?> clazz, String className, boolean ignoreCase) {
        if (null == clazz || Strings.isBlank(className)) {
            return false;
        }
        if (ignoreCase) {
            return className.equalsIgnoreCase(clazz.getName()) || className.equalsIgnoreCase(clazz.getSimpleName());
        } else {
            return className.equals(clazz.getName()) || className.equals(clazz.getSimpleName());
        }
    }

    public static Class<?> forName(String string, final ClassLoader classLoader) throws ClassNotFoundException {
        int arrayDimentions = 0;
        while (string.endsWith("[]")) {
            string = string.substring(0, string.length() - 2);
            arrayDimentions++;
        }

        Class<?> clazz = PRIMITIVES.get(string);

        if (clazz == null) {
            clazz = Class.forName(string, true, classLoader);
        }

        if (arrayDimentions == 0) {
            return clazz;
        }

        return Array.newInstance(clazz, new int[arrayDimentions]).getClass();
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> loadClass(String className, boolean isInitialized) {
        return (Class<T>) ClassLoaders.loadClass(className, isInitialized);
    }

    public static <T> Class<T> loadClass(String className) {
        return loadClass(className, true);
    }

    public static String packageName(final Class<?> clazz) {
        return packageName(clazz.getName());
    }

    public static String packageName(final String clazzName) {
        final int i = clazzName.lastIndexOf('.');
        if (i > 0) {
            return clazzName.substring(0, i);
        } else {
            return "";
        }
    }

    public static String simpleName(final Class<?> clazz) {
        return clazz.getSimpleName();
    }

    public static String simpleName(String clazzName) {
        int i = clazzName.lastIndexOf(46);
        return i > 0 ? clazzName.substring(i + 1) : clazzName;
    }

    public static List<String> simpleNames(final Class<?>... classes) {
        final List<String> list = new ArrayList<>();

        for (final Class<?> aClass : classes) {
            list.add(aClass.getSimpleName());
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createInstance(String clazz) {
        try {
            return (T) Class.forName(clazz).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Instance class [%s] error!", clazz), e);
        }
    }

    public static <T> T createInstance(Class<T> clazz, Object... params) {
        if (com.infilos.utils.Arrays.isEmpty(params)) {
            try {
                return (T) clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(String.format("Instance class [%s] error!", clazz), e);
            }
        }

        final Class<?>[] paramTypes = getClasses(params);
        final Constructor<?> constructor = findConstructor(clazz, paramTypes);
        if (null == constructor) {
            throw new RuntimeException(String.format("No Constructor matched for parameter types: [%s]", new Object[]{paramTypes}));
        }
        try {
            return findConstructor(clazz, paramTypes).newInstance(params);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Instance class [%s] error!", clazz), e);
        }
    }

    public static <T> T createIfPossible(Class<T> beanClass) {
        final Constructor<T>[] constructors = findConstructors(beanClass);
        Class<?>[] parameterTypes;
        for (Constructor<T> constructor : constructors) {
            parameterTypes = constructor.getParameterTypes();
            try {
                constructor.newInstance(TypeHelper.getDefaultValues(parameterTypes));
            } catch (Exception ignore) {
                // ignore exception and try next
            }
        }

        return null;
    }

    /**
     * Creates a list of the specified class and all its parent classes
     */
    public static List<Class<?>> findAncestors(Class<?> clazz) {
        final ArrayList<Class<?>> ancestors = new ArrayList<>();

        while (clazz != null && !clazz.equals(Object.class)) {
            ancestors.add(clazz);
            clazz = clazz.getSuperclass();
        }

        return ancestors;
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        if (null == clazz) {
            return null;
        }

        final Constructor<?>[] constructors = clazz.getConstructors();
        Class<?>[] pts;
        for (Constructor<?> constructor : constructors) {
            pts = constructor.getParameterTypes();
            if (TypeHelper.isAllAssignable(pts, parameterTypes)) {
                return (Constructor<T>) constructor;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T>[] findConstructors(Class<T> beanClass) throws SecurityException {
        Require.checkNotNull(beanClass);
        return (Constructor<T>[]) beanClass.getDeclaredConstructors();
    }

    public static Field findField(Class<?> beanClass, String name) throws SecurityException {
        final Field[] fields = findFields(beanClass, true);
        if (Arrays.isNotEmpty(fields)) {
            for (Field field : fields) {
                if ((name.equals(field.getName()))) {
                    return field;
                }
            }
        }

        return null;
    }

    public static Field[] findFields(Class<?> beanClass, boolean includeSuperClass) throws SecurityException {
        Require.checkNotNull(beanClass);

        Field[] allFields = null;
        Class<?> searchType = beanClass;
        Field[] declaredFields;
        while (searchType != null) {
            declaredFields = searchType.getDeclaredFields();
            if (null == allFields) {
                allFields = declaredFields;
            } else {
                allFields = com.infilos.utils.Arrays.append(allFields, declaredFields);
            }
            searchType = includeSuperClass ? searchType.getSuperclass() : null;
        }

        return allFields;
    }

    public static Field findDeclaredField(Class<?> clazz, String fieldName) throws SecurityException {
        if (null == clazz || Strings.isBlank(fieldName)) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignore) {
        }

        return null;
    }

    public static Field[] findDeclaredFields(Class<?> clazz) throws SecurityException {
        if (null == clazz) {
            return null;
        }
        return clazz.getDeclaredFields();
    }

    public static Object findFieldValue(Object obj, String fieldName) {
        if (null == obj || Strings.isBlank(fieldName)) {
            return null;
        }

        return findFieldValue(obj, findField(obj.getClass(), fieldName));
    }

    public static Object findFieldValue(Object obj, Field field) {
        if (null == obj || null == field) {
            return null;
        }

        field.setAccessible(true);
        Object result;
        try {
            result = field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("IllegalAccess for %s.%s", obj.getClass(), field.getName()), e);
        }

        return result;
    }

    public static void setupFieldValue(Object obj, String fieldName, Object value) {
        Require.checkNotNull(obj);
        Require.checkNotNull(fieldName);
        setupFieldValue(obj, findField(obj.getClass(), fieldName), value);
    }

    public static void setupFieldValue(Object obj, Field field, Object value) {
        Require.checkNotNull(obj);
        Require.checkNotNull(field);
        field.setAccessible(true);

        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("IllegalAccess for %s.%s", obj.getClass(), field.getName()), e);
        }
    }

    public static Method findObjectMethod(Object object, String methodName, Object... args) throws SecurityException {
        if (null == object || Strings.isBlank(methodName)) {
            return null;
        }
        return findMethod(object.getClass(), methodName, getClasses(args));
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) throws SecurityException {
        if (null == clazz || Strings.isBlank(methodName)) {
            return null;
        }

        final Iterable<Method> methods = ClassHelper.findMethods(clazz);
        if (Arrays.isNotEmpty(methods)) {
            for (Method method : methods) {
                if (methodName.equals(method.getName())) {
                    if (Arrays.isEmpty(paramTypes) || TypeHelper.isAllAssignable(method.getParameterTypes(), paramTypes)) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    public static Method findMethod(Class<?> beanClass, String name) throws SecurityException {
        for (Method method : ClassHelper.findMethods(beanClass)) {
            if ((name.equals(method.getName()))) {
                return method;
            }
        }
        return null;
    }

    public static Set<String> findMethodNames(Class<?> clazz) {
        final HashSet<String> methodSet = new HashSet<>();
        for (Method method : ClassHelper.findMethods(clazz)) {
            methodSet.add(method.getName());
        }
        return methodSet;
    }

    public static Iterable<Method> findMethods(final Class<?> clazz) {
        return new ArrayList<>(java.util.Arrays.asList(clazz.getMethods()));
    }

    public static Method[] findMethods(Class<?> beanClass, boolean withSuperClassMethods) throws SecurityException {
        Require.checkNotNull(beanClass);

        Method[] allMethods = null;
        Class<?> searchType = beanClass;
        Method[] declaredMethods;
        while (searchType != null) {
            declaredMethods = searchType.getDeclaredMethods();
            if (null == allMethods) {
                allMethods = declaredMethods;
            } else {
                allMethods = com.infilos.utils.Arrays.append(allMethods, declaredMethods);
            }
            searchType = withSuperClassMethods ? searchType.getSuperclass() : null;
        }

        return allMethods;
    }

    public static Set<String> findPublicMethodNames(Class<?> clazz) {
        HashSet<String> methodSet = new HashSet<String>();
        Method[] methodArray = findPublicMethods(clazz);
        for (Method method : methodArray) {
            String methodName = method.getName();
            methodSet.add(methodName);
        }
        return methodSet;
    }

    public static Method[] findPublicMethods(Class<?> clazz) {
        return clazz.getMethods();
    }

    public static Method findPublicMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) throws SecurityException {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static Set<String> findDeclaredMethodNames(Class<?> clazz) {
        return findMethodNames(clazz);
    }

    public static Method[] findDeclaredMethods(Class<?> clazz) {
        return findMethods(clazz, true);
    }

    public static Method findObjectDeclaredMethod(Object obj, String methodName, Object... args) throws SecurityException {
        return findDeclaredMethod(obj.getClass(), methodName, getClasses(args));
    }

    public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws SecurityException {
        return findMethod(clazz, methodName, parameterTypes);
    }

    public static boolean isEqualsMethod(Method method) {
        if (method == null || !method.getName().equals("equals")) {
            return false;
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        return (paramTypes.length == 1 && paramTypes[0] == Object.class);
    }

    public static boolean isHashCodeMethod(Method method) {
        return (method != null && method.getName().equals("hashCode") && method.getParameterTypes().length == 0);
    }

    public static boolean isToStringMethod(Method method) {
        return (method != null && method.getName().equals("toString") && method.getParameterTypes().length == 0);
    }

    public static Iterable<Parameter> findParams(final Method method) {
        return () -> new Iterator<Parameter>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < method.getParameterTypes().length;
            }

            @Override
            public Parameter next() {
                if (!hasNext()) throw new NoSuchElementException();

                return new Parameter(
                        method.getParameterAnnotations()[index],
                        method.getParameterTypes()[index],
                        method.getGenericParameterTypes()[index],
                        index++
                );
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterable<Parameter> findParams(final Constructor<?> constructor) {
        return () -> new Iterator<Parameter>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < constructor.getParameterTypes().length;
            }

            @Override
            public Parameter next() {
                if (!hasNext()) throw new NoSuchElementException();

                return new Parameter(
                        constructor.getParameterAnnotations()[index],
                        constructor.getParameterTypes()[index],
                        constructor.getGenericParameterTypes()[index],
                        index++
                );
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Class<?> findRawType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }

        return null;
    }

    public static Type findType(Field field) {
        if (null == field) {
            return null;
        }
        return field.getGenericType();
    }

    public static Type findParamType(Method method, int index) {
        Type[] types = findParamTypes(method);
        if (null != types && types.length > index) {
            return types[index];
        }
        return null;
    }

    public static Type[] findParamTypes(Method method) {
        if (null == method) {
            return null;
        }

        Type[] types = method.getGenericParameterTypes();
        if (com.infilos.utils.Arrays.isEmpty(types)) {
            types = method.getParameterTypes();
        }
        return types;
    }

    public static Type findReturnType(Method method) {
        if (null == method) {
            return null;
        }

        return method.getGenericReturnType();
    }

    public static Class<?> getTypeArgument(Class<?> clazz) {
        return getTypeArgument(clazz, 0);
    }

    public static Class<?> getTypeArgument(Class<?> clazz, int index) {
        final Type argumentType = findTypeArgumentOfType(clazz, index);
        if (argumentType instanceof Class) {
            return (Class<?>) argumentType;
        }
        return null;
    }

    public static Type findTypeArgumentOfClass(Class<?> clazz) {
        return findTypeArgumentOfType(clazz, 0);
    }

    public static Type findTypeArgumentOfClass(Class<?> clazz, int index) {
        return findTypeArgumentOfType(clazz.getGenericSuperclass(), index);
    }

    public static Type findTypeArgumentOfType(Type type) {
        return findTypeArgumentOfType(type, 0);
    }

    public static Type findTypeArgumentOfType(Type type, int index) {
        final Type[] typeArguments = findTypeArguments(type);
        if (null != typeArguments && typeArguments.length > index) {
            return typeArguments[index];
        }
        return null;
    }

    public static Type[] findTypeArguments(Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType genericSuperclass = (ParameterizedType) type;
            return genericSuperclass.getActualTypeArguments();
        }
        return null;
    }

    public static Type findIterableElementType(Class<?> clazz) {
        if (null == clazz) {
            return null;
        }

        Method nextMethod = null;
        try {
            if (Iterator.class.isAssignableFrom(clazz)) {
                nextMethod = clazz.getMethod("next");
            }
        } catch (NoSuchMethodException e) {
            return null;
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }

        return findReturnType(nextMethod);
    }
}
