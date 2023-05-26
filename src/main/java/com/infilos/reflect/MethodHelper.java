package com.infilos.reflect;

import com.infilos.utils.Strings;
import com.infilos.utils.Throws;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MethodHelper {
    private MethodHelper() {
    }

    public static <T> T invoke(String classNameDotMethodName, Object[] args) {
        return invoke(classNameDotMethodName, false, args);
    }

    public static <T> T invoke(String classNameWithMethodName, boolean isSingleton, Object... args) {
        if (Strings.isBlank(classNameWithMethodName)) {
            throw new RuntimeException("Blank classNameDotMethodName!");
        }

        int splitIndex = classNameWithMethodName.lastIndexOf('#');
        if (splitIndex <= 0) {
            splitIndex = classNameWithMethodName.lastIndexOf('.');
        }
        if (splitIndex <= 0) {
            throw new RuntimeException(String.format("Invalid classNameWithMethodName [%s]!", classNameWithMethodName));
        }

        final String className = classNameWithMethodName.substring(0, splitIndex);
        final String methodName = classNameWithMethodName.substring(splitIndex + 1);

        return invoke(className, methodName, isSingleton, args);
    }

    public static <T> T invoke(String className, String methodName, Object[] args) {
        return invoke(className, methodName, false, args);
    }

    public static <T> T invoke(String className, String methodName, boolean isSingleton, Object[] args) {
        Class<Object> clazz = ClassHelper.loadClass(className);
        try {
            final Method method = ClassHelper.findDeclaredMethod(clazz, methodName, ClassHelper.getClasses(args));
            if (null == method) {
                throw new NoSuchMethodException(String.format("No such method: [%s]", methodName));
            }
            if (TypeHelper.isStatic(method)) {
                return invoke(null, method, args);
            } else {
                return invoke(isSingleton ? Singletons.get(clazz) : clazz.newInstance(), method, args);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T invoke(Object obj, String methodName, Object[] args) {
        try {
            final Method method = ClassHelper.findObjectDeclaredMethod(obj, methodName, args);
            if (null == method) {
                throw new NoSuchMethodException(String.format("No such method: [%s]", methodName));
            }
            return invoke(obj, method, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T invokeStatic(Method method, Object... args) throws InvocationTargetException, IllegalArgumentException {
        return invoke(null, method, args);
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object obj, Method method, Object... args) throws InvocationTargetException, IllegalArgumentException {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return (T) method.invoke(TypeHelper.isStatic(method) ? null : obj, args);
        } catch (IllegalAccessException e) {
            throw Throws.runtime(e);
        }
    }
}
