package com.infilos.utils.differ;

import com.infilos.utils.Differ;

import java.util.*;

public class DifferBuilder {
    private final List<String> includeFields = new ArrayList<>();
    private final List<String> excludeFields = new ArrayList<>();
    private boolean bothExistOnly = true;

    public DifferBuilder include(String... fields) {
        includeFields.addAll(Arrays.asList(fields));
        return this;
    }

    public DifferBuilder exclude(String... fields) {
        excludeFields.addAll(Arrays.asList(fields));
        return this;
    }

    public DifferBuilder onlyBothExist(boolean bothExistOnly) {
        this.bothExistOnly = bothExistOnly;
        return this;
    }

    public Differ baseGetter() {
        return new GetterDiffer(bothExistOnly, includeFields, excludeFields);
    }

    public Differ baseField() {
        return new FieldDiffer(bothExistOnly, includeFields, excludeFields);
    }
}
