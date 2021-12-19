package com.infilos.utils.differ;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FieldDiffer extends BaseDiffer {
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public FieldDiffer() {
    }

    public FieldDiffer(boolean bothExistOnly) {
        super(bothExistOnly);
    }

    public FieldDiffer(List<String> includeFields, List<String> excludeFields) {
        super(includeFields, excludeFields);
    }

    public FieldDiffer(boolean bothExistOnly, List<String> includeFields, List<String> excludeFields) {
        super(bothExistOnly, includeFields, excludeFields);
    }

    @Override
    public List<FieldInfo> diff(Object first, Object second) {
        if (first == second) {
            return Collections.emptyList();
        }
        if (isBasicType(first, second)) {
            return compareBasicType(first, second);
        }
        if (isCollectionType(first, second)) {
            return compareCollectionType(first, second);
        }
        
        Set<String> allFieldNames;
        Map<String, Field> firstFields = scanFields(first);
        Map<String, Field> secondFields = scanFields(second);
        if (first == null) {
            allFieldNames = secondFields.keySet();
        } else if (second == null) {
            allFieldNames = firstFields.keySet();
        } else {
            allFieldNames = filterFieldNames(firstFields.keySet(), secondFields.keySet());
        }
        
        List<FieldInfo> diffFields = new LinkedList<>();
        for (String fieldName : allFieldNames) {
            try {
                Field firstField = firstFields.getOrDefault(fieldName, null);
                Field secondField = secondFields.getOrDefault(fieldName, null);
                Class<?> firstType = null;
                Class<?> secondType = null;
                Object firstValue = null;
                Object secondValue = null;
                
                if (firstField != null) {
                    firstField.setAccessible(true);
                    firstType = firstField.getType();
                    firstValue = firstField.get(first);
                }
                if (secondField != null) {
                    secondField.setAccessible(true);
                    secondType = secondField.getType();
                    secondValue = secondField.get(second);
                }
                FieldInfo fieldInfo = new FieldInfo(fieldName, firstType, secondType, firstValue, secondValue);
                
                if (!isFieldEquals(fieldInfo)) {
                    diffFields.add(fieldInfo);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Diff with field failed: " + fieldName, e);
            }
        }
        return diffFields;
    }

    private Map<String, Field> scanFields(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        return FIELD_CACHE.computeIfAbsent(obj.getClass(), k -> {
            Map<String, Field> fieldMap = new HashMap<>(8);
            Class<?> cls = k;
            while (cls != Object.class) {
                Field[] fields = cls.getDeclaredFields();
                for (Field field : fields) {
                    if (!field.isSynthetic()) {
                        fieldMap.put(field.getName(), field);
                    }
                }
                cls = cls.getSuperclass();
            }
            return fieldMap;
        });
    }
}
