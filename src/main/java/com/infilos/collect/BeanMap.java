package com.infilos.collect;

import com.infilos.reflect.SetAccessible;
import com.infilos.reflect.TypeHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Bean wrapper with Map operations support.
 */
public class BeanMap extends AbstractMap<String, Object> {
    private static final Pattern GETTER_PREFIX = Pattern.compile("(get|is|find)");
    private static final Method IS_RECORD;

    static {
        Method isRecord = null;
        try {
            //noinspection JavaReflectionMemberAccess
            isRecord = Class.class.getMethod("isRecord");
        } catch (NoSuchMethodException e) {
            // no-op
        }
        IS_RECORD = isRecord;
    }

    private final Object object;
    private final Map<String, Entry<String, Object>> attributes;
    private final Set<Entry<String, Object>> entries;

    public BeanMap(final Object object) {
        this(object.getClass(), object);
    }

    public BeanMap(final Class<?> clazz) {
        this(clazz, null);
    }

    public BeanMap(final Class<?> clazz, final Object object) {
        this.object = object;
        this.attributes = new HashMap<>();

        final boolean record = isRecordClass(clazz);
        if (!record) {
            initPOJOClass(clazz);
        } else {
            initRecord(clazz);
        }

        entries = Collections.unmodifiableSet(new HashSet<>(attributes.values()));
    }

    private void initRecord(final Class<?> clazz) {
        for (final Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                attributes.put(
                    field.getName(),
                    new MethodEntry("set" + field.getName(), clazz.getMethod(field.getName()), null)
                );
            } catch (final NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void initPOJOClass(final Class<?> clazz) {
        for (final Field field : clazz.getFields()) {
            if (field.isEnumConstant()) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            final FieldEntry entry = new FieldEntry(field);
            attributes.put(entry.getKey(), entry);
        }

        for (final Method getter : clazz.getMethods()) {
            if (!isValidGetter(getter)) {
                continue;
            }

            final String name = GETTER_PREFIX.matcher(getter.getName()).replaceFirst("set");
            final Method setter = getOptionalMethod(clazz, name, getter.getReturnType());
            final MethodEntry entry = new MethodEntry(name, getter, setter);

            attributes.put(entry.getKey(), entry);
        }
    }

    private boolean isRecordClass(final Class<?> clazz) {
        if (IS_RECORD == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(IS_RECORD.invoke(clazz));
        } catch (final IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    private boolean isValidGetter(Method m) {
        if (Modifier.isAbstract(m.getModifiers())) {
            return false;
        }
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }

        // Void methods are not valid getters
        if (Void.TYPE.equals(m.getReturnType())) {
            return false;
        }

        // Must have no parameters
        if (m.getParameterTypes().length != 0) {
            return false;
        }

        // Must start with "get" or "is"
        if (m.getName().startsWith("get") && m.getName().length() > 3) {
            return true;
        }
        if (m.getName().startsWith("find") && m.getName().length() > 4) {
            return true;
        }
        if (!m.getName().startsWith("is")) {
            return false;
        }

        // If it starts with "is" it must return boolean
        if (m.getReturnType().equals(Boolean.class)) {
            return true;
        }

        return m.getReturnType().equals(Boolean.TYPE);
    }

    private Method getOptionalMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException thisIsOk) {
            return null;
        }
    }

    @Override
    public Object get(final Object key) {
        final Entry<String, Object> entry = attributes.get(key);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }

    @Override
    public Object put(final String key, final Object value) {
        final Entry<String, Object> entry = attributes.get(key);
        if (entry == null) {
            return null;
        }

        return entry.setValue(value);
    }

    @Override
    public boolean containsKey(final Object key) {
        return attributes.containsKey(key);
    }

    @Override
    public Object remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return entries;
    }

    public class FieldEntry implements Member {
        private final Field field;

        public FieldEntry(final Field field) {
            this.field = field;
        }

        @Override
        public String getKey() {
            return field.getName();
        }

        @Override
        public Object getValue() {
            try {
                return field.get(object);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Object setValue(Object value) {
            try {
                final Object replaced = getValue();
                value = TypeHelper.castObject(value, field.getType(), getKey());
                field.set(object, value);
                return replaced;
            } catch (final IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public Class<?> getType() {
            return field.getType();
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }
    }

    public interface Member extends Entry<String, Object> {

        Class<?> getType();

        boolean isReadOnly();
    }

    public class MethodEntry implements Member {
        private final String key;
        private final Method getter;
        private final Method setter;

        public MethodEntry(final String methodName, final Method getter, final Method setter) {
            final StringBuilder name = new StringBuilder(methodName);

            // remove 'set' or 'get'
            name.delete(0, 3);

            // lowercase first char
            name.setCharAt(0, Character.toLowerCase(name.charAt(0)));

            this.key = name.toString();
            this.getter = getter;
            this.setter = setter;
        }

        protected Object invoke(final Method method, final Object... args) {
            SetAccessible.on(method);

            try {
                return method.invoke(object, args);
            } catch (final InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch (final Exception e) {
                throw new IllegalStateException(String.format("Key: %s, Method: %s", key, method.toString()), e);
            }
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return invoke(getter);
        }

        @Override
        public Object setValue(Object value) {
            if (setter == null) {
                throw new IllegalArgumentException(String.format("'%s' is read-only", key));
            }

            final Object original = getValue();
            value = TypeHelper.castObject(value, setter.getParameterTypes()[0], getKey());
            invoke(setter, value);

            return original;
        }

        @Override
        public Class<?> getType() {
            return getter.getReturnType();
        }

        @Override
        public boolean isReadOnly() {
            return setter != null;
        }
    }
}
