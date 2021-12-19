package com.infilos.utils.differ;

import java.util.Objects;

public class FieldInfo {
    private final String fieldName;
    private final Class<?> firstType;
    private final Class<?> secondType;
    private final Object firstValue;
    private final Object secondValue;

    FieldInfo(String fieldName,
              Class<?> firstType, Class<?> secondType,
              Object firstValue, Object secondValue) {
        this.fieldName = fieldName;
        this.firstType = firstType;
        this.secondType = secondType;
        this.firstValue = firstValue;
        this.secondValue = secondValue;
    }

    FieldInfo(String fieldName, Class<?> fieldType,
              Object firstValue, Object secondValue) {
        this.fieldName = fieldName;
        this.firstType = fieldType;
        this.secondType = fieldType;
        this.firstValue = firstValue;
        this.secondValue = secondValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getFirstType() {
        return firstType;
    }

    public Class<?> getSecondType() {
        return secondType;
    }

    public Object getFirstValue() {
        return firstValue;
    }

    public Object getSecondValue() {
        return secondValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldInfo fieldInfo = (FieldInfo) o;
        return Objects.equals(fieldName, fieldInfo.fieldName) &&
            Objects.equals(firstType, fieldInfo.firstType) &&
            Objects.equals(secondType, fieldInfo.secondType) &&
            Objects.equals(firstValue, fieldInfo.firstValue) &&
            Objects.equals(secondValue, fieldInfo.secondValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, firstType, secondType, firstValue, secondValue);
    }

    @Override
    public String toString() {
        return "FieldInfo{" +
            "fieldName='" + fieldName + '\'' +
            ", firstType=" + firstType +
            ", secondType=" + secondType +
            ", firstValue=" + firstValue +
            ", secondValue=" + secondValue +
            '}';
    }
}
