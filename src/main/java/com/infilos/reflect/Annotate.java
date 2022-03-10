package com.infilos.reflect;

import com.infilos.utils.Require;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public final class Annotate {
    private Annotate() {
    }

    public static List<Annotation> ofClass(Class<?> clazz, boolean inherited) {
        Require.check(Objects.nonNull(clazz), "Class must not be null.");
        
        List<Annotation> annotations = new ArrayList<>();

        Collections.addAll(annotations, clazz.getDeclaredAnnotations());
        if(!inherited) {
            return annotations;
        }

        for (Class<?> superClass = clazz; (superClass = superClass.getSuperclass()) != null; ) {
            Collections.addAll(annotations, superClass.getDeclaredAnnotations());
        }
        for (Class<?> superInterface : findAllInterfaces(clazz)) {
            Collections.addAll(annotations, superInterface.getDeclaredAnnotations());
        }
        
        return annotations;
    }

    public static List<Annotation> ofMethodDeclaringClass(Method method, boolean inherited) {
        Require.check(Objects.nonNull(method), "Method must not be null.");
        
        return ofClass(method.getDeclaringClass(), inherited);
    }

    public static List<Annotation> ofMethod(Method method, boolean inherited) {
        Require.check(Objects.nonNull(method), "Method must not be null.");
        
        List<Annotation> annotations = new ArrayList<>();

        Collections.addAll(annotations, method.getDeclaredAnnotations());
        if (!inherited) {
            return annotations;
        }

        Class<?> declareClass = method.getDeclaringClass();

        for (Class<?> superClass = declareClass; (superClass = superClass.getSuperclass()) != null; ) {
            findInheritMethodAnnotations(annotations, method, superClass);
        }
        for (Class<?> superInterface : findAllInterfaces(declareClass)) {
            findInheritMethodAnnotations(annotations, method, superInterface);
        }

        return annotations;
    }

    private static Set<Class<?>> findAllInterfaces(Class<?> clazz) {
        Set<Class<?>> set = new LinkedHashSet<>();

        do {
            findAllInterfaces(set, clazz);
        } while ((clazz = clazz.getSuperclass()) != null);

        return set;
    }

    private static void findAllInterfaces(Set<Class<?>> set, Class<?> c) {
        for (Class<?> i : c.getInterfaces()) {
            if (set.add(i)) {
                findAllInterfaces(set, i);
            }
        }
    }

    private static void findInheritMethodAnnotations(List<Annotation> collector, Method method, Class<?> superType) {
        try {
            Method superMethod = superType.getDeclaredMethod(method.getName(), method.getParameterTypes());
            if (isOverrides(method, superMethod)) {
                Collections.addAll(collector, superMethod.getDeclaredAnnotations());
            }
        } catch (NoSuchMethodException ignore) {
        }
    }

    /**
     * @param child  the method which may override parent.
     * @param parent the method which may be overridden by child.
     * @return true if child probably overrides parent and false otherwise.
     */
    public static boolean isOverrides(Method child, Method parent) {
        if (!child.getName().equals(parent.getName())) {
            return false;
        }

        Class<?> childClass = child.getDeclaringClass();
        Class<?> parentClass = parent.getDeclaringClass();
        if (childClass.equals(parentClass)) {
            return false;
        }
        if (!parentClass.isAssignableFrom(childClass)) {
            return false;
        }

        int childMods = child.getModifiers();
        int parentMods = parent.getModifiers();
        if (Modifier.isPrivate(childMods) || Modifier.isPrivate(parentMods)) {
            return false;
        }
        if (Modifier.isStatic(childMods) || Modifier.isStatic(parentMods)) {
            return false;
        }
        if (Modifier.isFinal(parentMods)) {
            return false;
        }
        if (compareAccess(childMods, parentMods) < 0) {
            return false;
        }
        if ((isPackageAccess(childMods) || isPackageAccess(parentMods))
            && !Objects.equals(childClass.getPackage(), parentClass.getPackage())) {
            return false;
        }
        if (!parent.getReturnType().isAssignableFrom(child.getReturnType())) {
            return false;
        }

        Class<?>[] childParams = child.getParameterTypes();
        Class<?>[] parentParams = parent.getParameterTypes();
        if (childParams.length != parentParams.length) {
            return false;
        }

        for (int i = 0; i < childParams.length; ++i) {
            if (!childParams[i].equals(parentParams[i])) {
                return false;
            }
        }

        return true;
    }

    private static boolean isPackageAccess(int mods) {
        return (mods & ACCESS_MODIFIERS) == 0;
    }

    private static final int ACCESS_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    private static final List<Integer> ACCESS_ORDER = Arrays.asList(
        Modifier.PRIVATE,
        0,
        Modifier.PROTECTED,
        Modifier.PUBLIC
    );

    private static int compareAccess(int lhs, int rhs) {
        return Integer.compare(
            ACCESS_ORDER.indexOf(lhs & ACCESS_MODIFIERS),
            ACCESS_ORDER.indexOf(rhs & ACCESS_MODIFIERS)
        );
    }
}
