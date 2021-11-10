package com.infilos.utils.sizes;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * Describes constant memory overheads for various constructs in a JVM implementation.
 */
public interface MemoryLayoutSpec {

    /**
     * Returns the fixed overhead of an array of any type or length in this JVM.
     *
     * @return the fixed overhead of an array.
     */
    int getArrayHeaderSize();

    /**
     * Returns the fixed overhead of for any {@link Object} subclass in this JVM.
     *
     * @return the fixed overhead of any object.
     */
    int getObjectHeaderSize();

    /**
     * Returns the quantum field size for a field owned by an object in this JVM.
     *
     * @return the quantum field size for an object.
     */
    int getObjectPadding();

    /**
     * Returns the fixed size of an object reference in this JVM.
     *
     * @return the size of all object references.
     */
    int getReferenceSize();

    /**
     * Returns the quantum field size for a field owned by one of an object's ancestor superclasses
     * in this JVM.
     *
     * @return the quantum field size for a superclass field.
     */
    int getSuperclassFieldPadding();


    static MemoryLayoutSpec resolveEffective() {
        final String vmName = System.getProperty("java.vm.name");
        if (vmName == null || !(vmName.startsWith("Java HotSpot(TM) ")
            || vmName.startsWith("OpenJDK") || vmName.startsWith("TwitterJDK"))) {
            throw new UnsupportedOperationException(
                "ObjectSizeCalculator only supported on HotSpot VM");
        }

        final String dataModel = System.getProperty("sun.arch.data.model");
        if ("32".equals(dataModel)) {
            // Running with 32-bit data model
            return new MemoryLayoutSpec() {
                @Override public int getArrayHeaderSize() {
                    return 12;
                }
                @Override public int getObjectHeaderSize() {
                    return 8;
                }
                @Override public int getObjectPadding() {
                    return 8;
                }
                @Override public int getReferenceSize() {
                    return 4;
                }
                @Override public int getSuperclassFieldPadding() {
                    return 4;
                }
            };
        } else if (!"64".equals(dataModel)) {
            throw new UnsupportedOperationException("Unrecognized value '" +
                dataModel + "' of sun.arch.data.model system property");
        }

        final String strVmVersion = System.getProperty("java.vm.version");
        final int vmVersion = Integer.parseInt(strVmVersion.substring(0, strVmVersion.indexOf('.')));
        if (vmVersion >= 17) {
            long maxMemory = 0;
            for (MemoryPoolMXBean mp : ManagementFactory.getMemoryPoolMXBeans()) {
                maxMemory += mp.getUsage().getMax();
            }
            if (maxMemory < 30L * 1024 * 1024 * 1024) {
                // HotSpot 17.0 and above use compressed OOPs below 30GB of RAM total
                // for all memory pools (yes, including code cache).
                return new MemoryLayoutSpec() {
                    @Override public int getArrayHeaderSize() {
                        return 16;
                    }
                    @Override public int getObjectHeaderSize() {
                        return 12;
                    }
                    @Override public int getObjectPadding() {
                        return 8;
                    }
                    @Override public int getReferenceSize() {
                        return 4;
                    }
                    @Override public int getSuperclassFieldPadding() {
                        return 4;
                    }
                };
            }
        }

        // In other cases, it's a 64-bit uncompressed OOPs object model
        return new MemoryLayoutSpec() {
            @Override public int getArrayHeaderSize() {
                return 24;
            }
            @Override public int getObjectHeaderSize() {
                return 16;
            }
            @Override public int getObjectPadding() {
                return 8;
            }
            @Override public int getReferenceSize() {
                return 8;
            }
            @Override public int getSuperclassFieldPadding() {
                return 8;
            }
        };
    }
}
