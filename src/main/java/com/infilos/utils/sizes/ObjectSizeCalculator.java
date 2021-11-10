package com.infilos.utils.sizes;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ObjectSizeCalculator {
    // Fixed object header size for arrays.
    private final int arrayHeaderSize;
    // Fixed object header size for non-array objects.
    private final int objectHeaderSize;
    // Padding for the object size - if the object size is not an exact multiple
    // of this, it is padded to the next multiple.
    private final int objectPadding;
    // Size of reference (pointer) fields.
    private final int referenceSize;
    // Padding for the fields of superclass before fields of subclasses are
    // added.
    private final int superclassFieldPadding;

    private final Map<Class<?>, ClassSizeInfo> classSizeInfos = new HashMap<>();

    private final Set<Object> alreadyVisited = Collections.newSetFromMap( new IdentityHashMap<>());
    private final Deque<Object> pending = new ArrayDeque<>(16 * 1024);
    private long size;

    /**
     * Creates an object size calculator that can calculate object sizes for a given
     * {@code memoryLayoutSpecification}.
     *
     * @param memoryLayoutSpec a description of the JVM memory layout.
     */
    public ObjectSizeCalculator(MemoryLayoutSpec memoryLayoutSpec) {
        Objects.requireNonNull(memoryLayoutSpec);
        arrayHeaderSize = memoryLayoutSpec.getArrayHeaderSize();
        objectHeaderSize = memoryLayoutSpec.getObjectHeaderSize();
        objectPadding = memoryLayoutSpec.getObjectPadding();
        referenceSize = memoryLayoutSpec.getReferenceSize();
        superclassFieldPadding = memoryLayoutSpec.getSuperclassFieldPadding();
    }

    /**
     * Given an object, returns the total allocated size, in bytes, of the object
     * and all other objects reachable from it.
     *
     * @param obj the object; can be null. Passing in a {@link java.lang.Class} object doesn't do
     *          anything special, it measures the size of all objects
     *          reachable through it (which will include its class loader, and by
     *          extension, all other Class objects loaded by
     *          the same loader, and all the parent class loaders). It doesn't provide the
     *          size of the static fields in the JVM class that the Class object
     *          represents.
     * @return the total allocated size of the object and all other objects it
     *         retains.
     */
    public synchronized long calculateObjectSize(Object obj) {
        // Breadth-first traversal instead of naive depth-first with recursive
        // implementation, so we don't blow the stack traversing long linked lists.
        try {
            for (;;) {
                visit(obj);
                if (pending.isEmpty()) {
                    return size;
                }
                obj = pending.removeFirst();
            }
        } finally {
            alreadyVisited.clear();
            pending.clear();
            size = 0;
        }
    }

    private void visit(Object obj) {
        if (alreadyVisited.contains(obj)) {
            return;
        }
        final Class<?> clazz = obj.getClass();
        if (clazz == ArrayElementsVisitor.class) {
            ((ArrayElementsVisitor) obj).visit(this);
        } else {
            alreadyVisited.add(obj);
            if (clazz.isArray()) {
                visitArray(obj);
            } else {
                buildClassSizeInfo(clazz).visit(obj, this);
            }
        }
    }

    private void visitArray(Object array) {
        final Class<?> componentType = array.getClass().getComponentType();
        final int length = Array.getLength(array);
        if (componentType.isPrimitive()) {
            increaseByArraySize(length, getPrimitiveFieldSize(componentType));
        } else {
            increaseByArraySize(length, referenceSize);
            // If we didn't use an ArrayElementsVisitor, we would be enqueueing every
            // element of the array here instead. For large arrays, it would
            // tremendously enlarge the queue. In essence, we're compressing it into
            // a small command object instead. This is different than immediately
            // visiting the elements, as their visiting is scheduled for the end of
            // the current queue.
            switch (length) {
                case 0: {
                    break;
                }
                case 1: {
                    enqueue(Array.get(array, 0));
                    break;
                }
                default: {
                    enqueue(new ArrayElementsVisitor((Object[]) array));
                }
            }
        }
    }

    private void increaseByArraySize(int length, long elementSize) {
        increaseSize(roundTo(arrayHeaderSize + length * elementSize, objectPadding));
    }

    void enqueue(Object obj) {
        if (obj != null) {
            pending.addLast(obj);
        }
    }

    void increaseSize(long objectSize) {
        size += objectSize;
    }

    private ObjectSizeCalculator.ClassSizeInfo buildClassSizeInfo(Class<?> clazz) {
        classSizeInfos.computeIfAbsent(clazz, c -> new ObjectSizeCalculator.ClassSizeInfo(clazz));

        return classSizeInfos.get(clazz);
    }

    private class ClassSizeInfo {
        // Padded fields + header size
        private final long objectSize;
        // Only the fields size - used to calculate the subclasses' memory
        // footprint.
        private final long fieldsSize;
        private final Field[] referenceFields;

        public ClassSizeInfo(Class<?> clazz) {
            long fieldsSize = 0;
            final List<Field> referenceFields = new LinkedList<>();
            for (Field f : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                final Class<?> type = f.getType();
                if (type.isPrimitive()) {
                    fieldsSize += getPrimitiveFieldSize(type);
                } else {
                    f.setAccessible(true);
                    referenceFields.add(f);
                    fieldsSize += referenceSize;
                }
            }
            final Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                final ObjectSizeCalculator.ClassSizeInfo superClassInfo = buildClassSizeInfo(superClass);
                fieldsSize += roundTo(superClassInfo.fieldsSize, superclassFieldPadding);
                referenceFields.addAll(Arrays.asList(superClassInfo.referenceFields));
            }
            this.fieldsSize = fieldsSize;
            this.objectSize = roundTo(objectHeaderSize + fieldsSize, objectPadding);
            this.referenceFields = referenceFields.toArray(new Field[referenceFields.size()]);
        }

        void visit(Object obj, ObjectSizeCalculator calc) {
            calc.increaseSize(objectSize);
            enqueueReferencedObjects(obj, calc);
        }

        public void enqueueReferencedObjects(Object obj, ObjectSizeCalculator calc) {
            for (Field f : referenceFields) {
                try {
                    calc.enqueue(f.get(obj));
                } catch (IllegalAccessException e) {
                    final AssertionError ae = new AssertionError(
                        "Unexpected denial of access to " + f);
                    ae.initCause(e);
                    throw ae;
                }
            }
        }
    }

    private static class ArrayElementsVisitor {
        private final Object[] array;

        ArrayElementsVisitor(Object[] array) {
            this.array = array;
        }

        public void visit(ObjectSizeCalculator calc) {
            for (Object elem : array) {
                if (elem!=null) {
                    calc.visit(elem);
                }
            }
        }
    }

    private static long roundTo(long x, int multiple) {
        return ((x + multiple - 1) / multiple) * multiple;
    }

    private static long getPrimitiveFieldSize(Class<?> type) {
        if (type == boolean.class || type == byte.class) {
            return 1;
        }
        if (type == char.class || type == short.class) {
            return 2;
        }
        if (type == int.class || type == float.class) {
            return 4;
        }
        if (type == long.class || type == double.class) {
            return 8;
        }
        throw new AssertionError("Encountered unexpected primitive type " + type.getName());
    }
}
