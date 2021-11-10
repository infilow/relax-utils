package com.infilos.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataSize implements Comparable<DataSize> {
    private final Double value;
    private final DataUnit unit;

    public DataSize(Double value, DataUnit unit) {
        Require.check(!value.isInfinite(), "infinite size");
        Require.check(!value.isNaN(), "size is not a number");
        Require.check(value >= 0, "negative size: ", value, unit);

        this.value = value;
        this.unit = unit;
    }

    public Double value() {
        return value;
    }

    public DataUnit unit() {
        return unit;
    }

    public long toBytes() {
        Double bytes = valueOf(DataUnit.BYTE);
        Require.check(bytes <= Long.MAX_VALUE, "too large to represent in bytes in Long type: " + this);

        return bytes.longValue();
    }

    public Double valueOf(DataUnit unit) {
        return value * (this.unit.factor() * 1.0 / unit.factor());
    }

    public DataSize convertTo(DataUnit unit) {
        return new DataSize(valueOf(unit), unit);
    }

    public DataSize succinctDataSize() {
        DataUnit targetUnit = findMostSuccinctUnit(DataUnit.BYTE, Units.subList(1, Units.size()));
        return convertTo(targetUnit);
    }

    private DataUnit findMostSuccinctUnit(DataUnit unit, List<DataUnit> remaining) {
        if(remaining.isEmpty()) {
            return unit;
        } else {
            DataUnit nextUnit = remaining.get(0);
            if(valueOf(nextUnit) < 1.0) {
                return unit;
            } else {
                return findMostSuccinctUnit(nextUnit, remaining.subList(1, remaining.size()));
            }
        }
    }

    public Long roundTo(DataUnit unit) {
        double rounded = Math.floor(valueOf(unit) + 0.5D);
        Require.check(rounded <= Long.MAX_VALUE, "%s is too large to represent in %s in Long type", this, unit.unitString());

        return ((Double) rounded).longValue();
    }

    @Override
    public int compareTo(DataSize o) {
        return valueOf(DataUnit.BYTE).compareTo(o.valueOf(DataUnit.BYTE));
    }

    @Override
    public String toString() {
        // Has fraction?
        if(Math.floor(value) == value) {
            return ((Double)Math.floor(value)).longValue() + unit.unitString();
        } else {
            return String.format("%.2f%s", value, unit.unitString());
        }
    }

    public static DataSize of(long byteSize) {
        return new DataSize((double) byteSize, DataUnit.BYTE);
    }

    public static DataSize ofSuccinct(long byteSize) {
        return new DataSize((double) byteSize, DataUnit.BYTE).succinctDataSize();
    }

    private static final Pattern PATTERN = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)\\s*$");

    public static DataSize parse(String dataSizeString) {
        Matcher matcher = PATTERN.matcher(dataSizeString);
        while (matcher.find()) {
            if(matcher.groupCount() == 2) {
                Double value = Double.valueOf(matcher.group(1));
                DataUnit unit = UnitTable.get(matcher.group(2));
                return new DataSize(value, unit);
            }
        }

        try {
            long value = Long.parseLong(dataSizeString);
            return new DataSize((double) value, DataUnit.BYTE);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Invalid data size string: " + dataSizeString);
        }
    }

    public enum DataUnit {
        BYTE(1L, "B"),
        KILOBYTE(1L << 10, "kB"),
        MEGABYTE(1L << 20, "MB"),
        GIGABYTE(1L << 30, "GB"),
        TERABYTE(1L << 40, "TB"),
        PETABYTE(1L << 50, "PB");

        private final long factor;
        private final String unit;

        DataUnit(long factor, String unit) {
            this.factor = factor;
            this.unit = unit;
        }

        public long factor() {
            return factor;
        }

        public String unitString() {
            return unit;
        }
    }

    public static final List<DataUnit> Units =
        Collections.unmodifiableList(Arrays.asList(
            DataUnit.BYTE, DataUnit.KILOBYTE, DataUnit.MEGABYTE, DataUnit.GIGABYTE, DataUnit.TERABYTE, DataUnit.PETABYTE));

    public static final Map<String, DataUnit> UnitTable =
        Units.stream().collect(Collectors.toMap(DataUnit::unitString, Function.identity()));
}
