package com.infilos.reflect;

import org.junit.Test;

import java.lang.annotation.*;

import static org.junit.Assert.*;

public class AnnotateTest {

    @Test
    public void testClass() {
        Annotate.ofClass(Class2.class, true).forEach(System.out::println);
    }

    @Test
    public void testMethodClass() throws NoSuchMethodException {
        Annotate.ofMethodDeclaringClass(Class2.class.getMethod("num"), true).forEach(System.out::println);
    }

    @Test
    public void testMethod() throws NoSuchMethodException {
        Annotate.ofMethod(Class2.class.getMethod("num"), true).forEach(System.out::println);
    }

    @Inherited
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Annot1 {
        int num();
    }

    @Annot1(num = 5)
    private static class Class1 {
        @Annot1(num = 5)
        public int num() {
            return 1;
        }
    }

    @Inherited
    @Target({ElementType.TYPE,ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annot2 {
        String text();
    }

    @Annot2(text = "iii")
    private interface TheInterface {
        @Annot2(text = "iii")
        int num();
    }

    @Annot2(text = "ccc")
    private static class Class2 extends Class1 implements TheInterface {
        @Annot2(text = "ccc")
        public int num() {
            return super.num() + 1;
        }
    }
}