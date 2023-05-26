package com.infilos.reflect;

import org.junit.Assert;
import org.junit.Test;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.PrintStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class ClassHelperTest extends Assert {

    @Test
    public void testConvert() throws Exception {
        assertEquals(1, TypeHelper.castObject("1", Integer.class, null));
        assertEquals(1L, TypeHelper.castObject("1", Long.class, null));
        assertEquals(true, TypeHelper.castObject("true", Boolean.class, null));
        assertEquals(new URI("foo"), TypeHelper.castObject("foo", URI.class, null));
        assertEquals(new Green("foo"), TypeHelper.castObject("foo", Green.class, null));

        final Yellow expected = new Yellow();
        expected.value = "foo";

        assertEquals(expected, TypeHelper.castObject("foo", Yellow.class, null));
    }

    @Test
    public void testConvertString() {
        assertEquals(
            asList("a", "b", "c"),
                TypeHelper.castString("a, b, c", new ParameterizedTypeImpl(List.class, String.class), "foo"));
        assertEquals(
            new HashSet<>(asList("a", "b", "c")),
                TypeHelper.castString("a, b, c", new ParameterizedTypeImpl(Set.class, String.class), "foo"));
        assertEquals(
            asList(1, 2, 3),
                TypeHelper.castString("1, 2, 3", new ParameterizedTypeImpl(Collection.class, Integer.class), "foo"));
        assertEquals(
            new HashMap<Integer, String>() {{
                put(1, "a");
                put(2, "b");
                put(3, "c");
            }},
                TypeHelper.castString("1=a\n2=b\n3=c\n", new ParameterizedTypeImpl(Map.class, Integer.class, String.class), "foo"));
    }

    @Test
    public void testForName() throws Exception {
        final Class<?> clazz = ClassHelper.forName(ClassHelperTest.class.getName(), ClassHelperTest.class.getClassLoader());
        assertEquals(ClassHelperTest.class, clazz);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testForNameNotFound() throws Exception {
        final Class<?> clazz = ClassHelper.forName(ClassHelperTest.class.getName() + "s", ClassHelperTest.class.getClassLoader());
        assertEquals(ClassHelperTest.class, clazz);
    }

    @Test
    public void testPackageName() throws Exception {
        final String s = ClassHelper.packageName(ClassHelperTest.class);
        assertEquals("com.infilos.reflect", s);

        // Class from the Default package
        final Class<?> goodCatch = ClassHelper.forName("com.infilos.reflect.GoodCatch", Thread.currentThread().getContextClassLoader());
        assertEquals("com.infilos.reflect", ClassHelper.packageName(goodCatch));
    }

    @Test
    public void testPackageName1() throws Exception {
        final String s = ClassHelper.packageName(ClassHelperTest.class.getName());
        assertEquals("com.infilos.reflect", s);

        // Class from the Default package
        final Class<?> goodCatch = ClassHelper.forName("com.infilos.reflect.GoodCatch", Thread.currentThread().getContextClassLoader());
        assertEquals("com.infilos.reflect", ClassHelper.packageName(goodCatch.getName()));
    }

    @Test
    public void testSimpleName() throws Exception {
        final String s = ClassHelper.simpleName(ClassHelperTest.class.getName());
        assertEquals("ClassHelperTest", s);

        // Class from the Default package
        final Class<?> goodCatch = ClassHelper.forName("com.infilos.reflect.GoodCatch", Thread.currentThread().getContextClassLoader());
        assertEquals("GoodCatch", ClassHelper.simpleName(goodCatch.getName()));
    }

    @Test
    public void testSimpleName1() throws Exception {
        assertEquals("ClassHelperTest", ClassHelper.simpleName(ClassHelperTest.class));

        // Class from the Default package
        final Class<?> goodCatch = ClassHelper.forName("com.infilos.reflect.GoodCatch", Thread.currentThread().getContextClassLoader());
        assertEquals("GoodCatch", ClassHelper.simpleName(goodCatch));
    }

    @Test
    public void testGetSimpleNames() {
        final List<String> simpleNames = ClassHelper.simpleNames(String.class, ClassHelper.class, Integer.class);

        final Iterator<String> iterator = simpleNames.iterator();
        assertEquals("String", iterator.next());
        assertEquals("ClassHelper", iterator.next());
        assertEquals("Integer", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testDeprimitivize() {
        assertEquals(Byte.class, TypeHelper.boxType(byte.class));
        assertEquals(Short.class, TypeHelper.boxType(short.class));
        assertEquals(Integer.class, TypeHelper.boxType(int.class));
        assertEquals(Long.class, TypeHelper.boxType(long.class));
        assertEquals(Float.class, TypeHelper.boxType(float.class));
        assertEquals(Double.class, TypeHelper.boxType(double.class));
        assertEquals(Character.class, TypeHelper.boxType(char.class));
        assertEquals(Boolean.class, TypeHelper.boxType(boolean.class));
    }

    @Test
    public void testAncestors() {
        final List<Class<?>> ancestors = ClassHelper.findAncestors(PrintStream.class);

        final Iterator<Class<?>> iterator = ancestors.iterator();
        assertEquals("java.io.PrintStream", iterator.next().getName());
        assertEquals("java.io.FilterOutputStream", iterator.next().getName());
        assertEquals("java.io.OutputStream", iterator.next().getName());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testEnumEditor() {
        PropertyEditorManager.registerEditor(TimeUnit.class, TimeUnitEditor.class);
        final Optional<PropertyEditor> editor = PropertyEditors.of(TimeUnit.class);
        assertTrue(editor.isPresent());

        final Object o = TypeHelper.castObject("horas", TimeUnit.class, "time");
        assertEquals(TimeUnit.HOURS, o);
    }

    public static class ParameterizedTypeImpl implements ParameterizedType {
        private final Type raw;
        private final Type[] arg;

        public ParameterizedTypeImpl(final Type raw, final Type... arg) {
            this.raw = raw;
            this.arg = arg;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return arg;
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    public static class Green {

        private final String value;

        public Green(final String value) {
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Green)) {
                return false;
            }

            final Green green = (Green) o;

            return Objects.equals(value, green.value);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    public static class Yellow {

        private String value;

        public static Yellow makeOne(final String value) {
            final Yellow yellow = new Yellow();
            yellow.value = value;
            return yellow;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Yellow yellow = (Yellow) o;

            return Objects.equals(value, yellow.value);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    public static class Key {
        private final String k;

        public Key(String pwz) {
            k = pwz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            return k.equals(key.k);

        }

        @Override
        public int hashCode() {
            return k.hashCode();
        }

        @Override
        public String toString() {
            return "Key{" +
                "k='" + k + '\'' +
                '}';
        }
    }

    public static class TimeUnitEditor extends java.beans.PropertyEditorSupport {

        @Override
        public void setAsText(final String text) throws IllegalArgumentException {
            setValue(toObjectImpl(text));
        }
        
        protected Object toObjectImpl(String text) {
            if ("horas".equals(text)) return TimeUnit.HOURS;
            if ("dias".equals(text)) return TimeUnit.DAYS;
            return TimeUnit.valueOf(text);
        }
    }
}