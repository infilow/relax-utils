package com.infilos.utils.differ;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GetterDiffer extends BaseDiffer {
    private static final String GET = "get";
    private static final String IS = "is";
    private static final String GET_IS = "get|is";
    private static final String GET_CLASS = "getClass";
    private static final Map<Class<?>, Map<String, Method>> FIELD_CACHE = new ConcurrentHashMap<>();

    public GetterDiffer() {
    }

    public GetterDiffer(boolean bothExistOnly) {
        super(bothExistOnly);
    }

    public GetterDiffer(List<String> includeFields, List<String> excludeFields) {
        super(includeFields, excludeFields);
    }

    public GetterDiffer(boolean bothExistOnly, List<String> includeFields, List<String> excludeFields) {
        super(bothExistOnly, includeFields, excludeFields);
    }

    @Override
    public List<FieldInfo> diff(Object first, Object second) {
        if (first == null && second == null) {
            return Collections.emptyList();
        }
        if (isBasicType(first, second)) {
            return compareBasicType(first, second);
        }
        if (isCollectionType(first, second)) {
            return compareCollectionType(first, second);
        }

        Set<String> allFieldNames;
        Map<String, Method> firstGetters = scanGetters(first);
        Map<String, Method> secondGetters = scanGetters(second);
        if (first == null) {
            allFieldNames = secondGetters.keySet();
        } else if (second == null) {
            allFieldNames = firstGetters.keySet();
        } else {
            allFieldNames = filterFieldNames(firstGetters.keySet(), secondGetters.keySet());
        }

        List<FieldInfo> diffFields = new LinkedList<>();
        for (String fieldName : allFieldNames) {
            try {
                Method firstGetterMethod = firstGetters.getOrDefault(fieldName, null);
                Method secondGetterMethod = secondGetters.getOrDefault(fieldName, null);
                Object firstValue = firstGetterMethod != null ? firstGetterMethod.invoke(first) : null;
                Object secondValue = secondGetterMethod != null ? secondGetterMethod.invoke(second) : null;
                FieldInfo fieldInfo = new FieldInfo(fieldName, getReturnType(firstGetterMethod), getReturnType(secondGetterMethod), firstValue, secondValue);

                if (!isFieldEquals(fieldInfo)) {
                    diffFields.add(fieldInfo);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Diff with getter failed: " + fieldName, e);
            }
        }
        
        return diffFields;
    }

    private Class<?> getReturnType(Method method) {
        return method == null ? null : method.getReturnType();
    }

    private Map<String, Method> scanGetters(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }

        return FIELD_CACHE.computeIfAbsent(obj.getClass(), k -> {
            Class<?> clazz = obj.getClass();
            Map<String, Method> getters = new LinkedHashMap<>(8);
            while (clazz != Object.class) {
                Method[] methods = clazz.getDeclaredMethods();
                for (Method m : methods) {
                    if (!Modifier.isPublic(m.getModifiers()) || m.getParameterTypes().length > 0) {
                        continue;
                    }

                    // IS
                    if (m.getReturnType() == Boolean.class || m.getReturnType() == boolean.class) {
                        if (m.getName().startsWith(IS)) {
                            String fieldName = uncapitalize(m.getName().substring(2));
                            getters.put(fieldName, m);
                            continue;
                        }
                    }
                    // GET
                    if (m.getName().startsWith(GET) && !GET_CLASS.equals(m.getName())) {
                        String fieldName = uncapitalize(m.getName().replaceFirst(GET_IS, ""));
                        getters.put(fieldName, m);
                    }
                }
                clazz = clazz.getSuperclass();
            }

            return getters;
        });
    }

    /**
     * From commons-lang3.
     */
    private String uncapitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toLowerCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            return str;
        }
        final int[] newCodePoints = new int[strLen];
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint;
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen; ) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint;
            inOffset += Character.charCount(codepoint);
        }

        return new String(newCodePoints, 0, outOffset);
    }
}
