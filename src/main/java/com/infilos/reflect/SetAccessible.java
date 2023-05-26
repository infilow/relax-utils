package com.infilos.reflect;

import java.lang.reflect.AccessibleObject;
import java.security.AccessController;
import java.security.PrivilegedAction;

@SuppressWarnings("ALL")
public class SetAccessible implements PrivilegedAction {
    private final AccessibleObject object;

    public SetAccessible(final AccessibleObject object) {
        this.object = object;
    }

    public Object run() {
        object.setAccessible(true);
        return object;
    }

    public static <T extends AccessibleObject> T on(final T object) {
        return (T) AccessController.doPrivileged(new SetAccessible(object));
    }
}
