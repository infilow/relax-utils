package com.infilos.reflect;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ClassFieldTest {

    @Test
    public void of_validFieldsWithGetters_expectReturnsFieldNames() {
        assertEquals("myField", ClassField.nameOf(TestClass.class, TestClass::getMyField));
        assertEquals("anotherField", ClassField.nameOf(TestClass.class, TestClass::isAnotherField));
        assertEquals("somethingElse", ClassField.nameOf(TestClass.class, TestClass::getSomethingElse));
        assertEquals("yetAnotherField", ClassField.nameOf(TestClass.class, TestClass::getYetAnotherField));
        assertEquals("byteField", ClassField.nameOf(TestClass.class, TestClass::getByteField));
        assertEquals("shortField", ClassField.nameOf(TestClass.class, TestClass::getShortField));
        assertEquals("someMethod", ClassField.nameOf(TestClass.class, TestClass::someMethod));
        assertEquals("someOne", ClassField.nameOf(TestClass.class, TestClass::getSomeOne));
    }

    @Test
    public void of_nullParameters_expectThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ClassField.nameOf(null, TestClass::someMethod));
        assertThrows(IllegalArgumentException.class, () -> ClassField.nameOf(TestClass.class, null));
    }

    private static class SomeOne {
    }

    private static class TestClass {
        private String myField;
        private boolean anotherField;
        private Boolean yetAnotherField;

        private int somethingElse;
        private byte byteField;
        private Short shortField;

        private SomeOne someOne;

        public TestClass() {
        }

        public String getMyField() {
            return myField;
        }

        public boolean isAnotherField() {
            return anotherField;
        }

        public int getSomethingElse() {
            return somethingElse;
        }

        public Boolean getYetAnotherField() {
            return yetAnotherField;
        }

        public byte getByteField() {
            return byteField;
        }

        public Short getShortField() {
            return shortField;
        }

        public int someMethod() {
            return 0;
        }

        public SomeOne getSomeOne() {
            return someOne;
        }
    }
}