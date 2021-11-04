package com.infilos.utils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public final class TimeSpan {
    private final String tag;
    private final long start;
    private final long stop;

    TimeSpan(String tag, long start, long stop) {
        this.tag = tag;
        this.start = start;
        this.stop = stop;
    }
    
    public String tag() {
        return tag;
    }

    public long costMillis() {
        return (stop - start) / 1000000;
    }

    public long costNanos() {
        return stop - start;
    }

    public Duration costDuration() {
        return Duration.of(stop - start, ChronoUnit.NANOS);
    }

    public String costString() {
        return Timer.formatReadable(costMillis());
    }
}
