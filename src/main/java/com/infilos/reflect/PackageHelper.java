package com.infilos.reflect;

public final class PackageHelper {
    private PackageHelper() {
    }

    public static String findPackage(Class<?> clazz) {
        if (clazz == null) {
            return "";
        }
        final String className = clazz.getName();
        int packageEndIndex = className.lastIndexOf(".");
        if (packageEndIndex == -1) {
            return "";
        }
        return className.substring(0, packageEndIndex);
    }

    public static String findPackagePath(Class<?> clazz) {
        return findPackage(clazz).replace(".", "/");
    }
}
