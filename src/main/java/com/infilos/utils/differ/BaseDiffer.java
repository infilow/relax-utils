package com.infilos.utils.differ;

import com.infilos.utils.Differ;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseDiffer implements Differ {
    private static final List<Class<?>> WRAPPER = Arrays.asList(
        Byte.class, 
        Short.class,
        Integer.class, 
        Long.class,
        Float.class,
        Double.class,
        Character.class,
        Boolean.class, 
        String.class
    );
    
    private final List<String> includeFields = new ArrayList<>();
    private final List<String> excludeFields = new ArrayList<>();
    private boolean bothExistOnly = true;
    
    public BaseDiffer() {
    }

    public BaseDiffer(boolean bothExistOnly) {
        this.bothExistOnly = bothExistOnly;
    }

    public BaseDiffer(List<String> includeFields, List<String> excludeFields) {
        this.includeFields.addAll(includeFields);
        this.excludeFields.addAll(excludeFields);
    }

    public BaseDiffer(boolean bothExistOnly,List<String> includeFields, List<String> excludeFields) {
        this.bothExistOnly = bothExistOnly;
        this.includeFields.addAll(includeFields);
        this.excludeFields.addAll(excludeFields);
    }

    @Override
    public boolean isEqual(Object first, Object second) {
        List<FieldInfo> differences = diff(first, second);
        
        return differences == null || differences.isEmpty();
    }

    protected boolean isFieldEquals(FieldInfo fieldInfo) {
        // 先判断排除，如果需要排除，则无论在不在包含范围，都一律不比对
        if (isExclude(fieldInfo)) {
            return true;
        }
        // 如果有指定需要包含的字段而且当前字段不在需要包含的字段中则不比对
        if (!isInclude(fieldInfo)) {
            return true;
        }
        return isNullableEquals(fieldInfo.getFirstValue(), fieldInfo.getSecondValue());
    }

    protected boolean isInclude(FieldInfo fieldInfo) {
        if (includeFields.isEmpty()) {
            return true;
        }
        
        return includeFields.contains(fieldInfo.getFieldName());
    }

    protected boolean isExclude(FieldInfo fieldInfo) {
        // 如果有指定需要排除的字段，而且当前字段是需要排除字段，则直接返回 true
        return !excludeFields.isEmpty() && excludeFields.contains(fieldInfo.getFieldName());
    }

    boolean isBasicType(Object first, Object second) {
        Object obj = first == null ? second : first;
        Class<?> clazz = obj.getClass();

        return clazz.isPrimitive() || WRAPPER.contains(clazz);
    }

    List<FieldInfo> compareBasicType(Object first, Object second) {
        boolean eq = Objects.equals(first, second);
        
        if (eq) {
            return Collections.emptyList();
        } else {
            Object obj = first == null ? second : first;
            Class<?> clazz = obj.getClass();

            return Collections.singletonList(new FieldInfo(clazz.getSimpleName(), clazz, first, second));
        }
    }
    
    boolean isCollectionType(Object first, Object second) {
        Object obj = first == null ? second : first;
        
        return obj instanceof Collection;
    }
    
    List<FieldInfo> compareCollectionType(Object first, Object second) {
        boolean eq = Objects.deepEquals(first, second);

        if (eq) {
            return Collections.emptyList();
        } else {
            Object obj = first == null ? second : first;
            Class<?> clazz = obj.getClass();

            return Collections.singletonList(new FieldInfo(clazz.getSimpleName(), clazz, first, second));
        }
    }
    
    Set<String> filterFieldNames(Set<String> firstFields, Set<String> secondFields) {
        if (bothExistOnly) {
            return firstFields.stream().filter(secondFields::contains).collect(Collectors.toSet());
        } else {
            return new HashSet<String>(){{
                addAll(firstFields);
                addAll(secondFields);
            }};
        }
    }
    
    @SuppressWarnings("rawtypes")
    private boolean isNullableEquals(Object first, Object second) {
        if (first instanceof Collection && second instanceof Collection) {
            return Objects.deepEquals(((Collection) first).toArray(), ((Collection) second).toArray());
        }
        
        return Objects.deepEquals(first, second);
    }
}
