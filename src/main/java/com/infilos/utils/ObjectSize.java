package com.infilos.utils;

import com.infilos.utils.sizes.MemoryLayoutSpec;
import com.infilos.utils.sizes.ObjectSizeCalculator;

/**
 * https://stackoverflow.com/questions/52353/in-java-what-is-the-best-way-to-determine-the-size-of-an-object
 */
public final class ObjectSize {
    private ObjectSize(){
    }

    /**
     * Get maximum size of heap in bytes. The heap cannot grow beyond this size.
     * Any attempt will result in an OutOfMemoryException.
     */
    public static long ofMaxJvmHeap() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * Get current size of heap in bytes.
     */
    public static long ofUsedJvmHeap() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * Get amount of free memory within the heap in bytes. This size will increase
     * after garbage collection and decrease as new objects are created.
     */
    public static long ofFreeJvmHeap() {
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * Get size of object.
     */
    public static long of(Object object) {
        return object == null ? 0 : new ObjectSizeCalculator(CurrentLayout.SPEC).calculateObjectSize(object);
    }

    private static class CurrentLayout {
        private static final MemoryLayoutSpec SPEC = MemoryLayoutSpec.resolveEffective();
    }
}
