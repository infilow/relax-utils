package com.infilos.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

public class Parameter implements AnnotatedElement {

    private final Annotation[] annotations;
    private final Class<?> type;
    private final Type genericType;
    private final int index;

    public Parameter(final Annotation[] annotations, final Class<?> type, final Type genericType) {
        this(annotations, type, genericType, -1);
    }

    public Parameter(final Annotation[] annotations, final Class<?> type, final Type genericType, final int index) {
        this.annotations = annotations;
        this.type = type;
        this.genericType = genericType;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public Class<?> getType() {
        return type;
    }

    public Type getGenericType() {
        return genericType;
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        for (final Annotation annotation : annotations) {
            if (annotationClass.equals(annotation.annotationType())) {
                return (T) annotation;
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }
}