package com.infilos.utils;

import com.infilos.utils.differ.*;

import java.util.*;

public interface Differ {

    boolean isEqual(Object first, Object second);

    List<FieldInfo> diff(Object first, Object second);

    static Differ baseGetter() {
        return new GetterDiffer();
    }

    static Differ baseField() {
        return new FieldDiffer();
    }

    static DifferBuilder builder() {
        return new DifferBuilder();
    }
}
