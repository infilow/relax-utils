package com.infilos.reflect;

import com.infilos.utils.Require;
import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

/**
 * Java 8 version: https://github.com/mobiuscode-de/nameof
 */
public final class ClassField {
    private ClassField() {
    }

    public static final String GETTER_PREFIX_GET = "get";
    public static final String GETTER_PREFIX_IS = "is";

    /**
     * programmatically returns the name of the type's field specified by the given getter method
     *
     * <p>Example:
     *
     * <pre>
     *   nameOf(MyClass.class, MyClass::getFoobar);
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public static <T> String nameOf(Class<T> typeClass, Function<T, ?> entityFieldGetter) {
        Require.check(Objects.nonNull(typeClass), "unable to determine name of null class");
        Require.check(Objects.nonNull(entityFieldGetter), "unable to determine name of null getter reference");

        try {
            StringBuilder fieldName = new StringBuilder();
            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setSuperclass(typeClass);

            T fakeEntity = (T) proxyFactory.create(
                new Class<?>[0],
                new Object[0],
                ((o, method, method1, objects) -> {
                    fieldName.append(getterToFieldName(method.getName()));
                    return getValidReturnValue(method);
                })
            );

            entityFieldGetter.apply(fakeEntity);

            return fieldName.toString();
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("unable to determine name of field for provided getter", e);
        }
    }

    private static Object getValidReturnValue(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.isPrimitive()) {
            if (returnType == boolean.class || returnType == Boolean.class) {
                return false;
            }
            if (returnType == byte.class || returnType == Byte.class) {
                return (byte) 0;
            }
            if (returnType == char.class || returnType == Character.class) {
                return (char) 0;
            }
            if (returnType == short.class || returnType == Short.class) {
                return (short) 0;
            }
            if (returnType == long.class || returnType == Long.class) {
                return (long) 0;
            }
            return 0;
        }

        return null;
    }

    private static String getterToFieldName(String methodName) {
        int cutOffLength = 0;
        if (methodName.startsWith(GETTER_PREFIX_GET)) {
            cutOffLength = 3;
        } else if (methodName.startsWith(GETTER_PREFIX_IS)) {
            cutOffLength = 2;
        }

        if (methodName.length() <= cutOffLength + 1) {
            throw new IllegalArgumentException(
                String.format("method name \"%s\" is not according to convention for a getter", methodName));
        }

        return Character.toLowerCase(methodName.charAt(cutOffLength)) + methodName.substring(cutOffLength + 1);
    }
}
