package com.infilos.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public final class Datetimes {
    private Datetimes() {
    }

    public static long nowInMills() {
        return System.currentTimeMillis();
    }

    public static long nowInNanos() {
        return System.nanoTime();
    }

    public static long nowInMillsOfNano() {
        return TimeUnit.NANOSECONDS.toMillis(nowInNanos());
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static LocalDateTime from(String datetime, DateTimeFormatter pattern) {
        return LocalDateTime.parse(datetime, pattern);
    }

    public static LocalDateTime from(long timestamp) {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId());
    }

    public static long asMillis(LocalDateTime datetime) {
        return datetime.atZone(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli();
    }

    public static long asSeconds(LocalDateTime datetime) {
        return TimeUnit.MILLISECONDS.toSeconds(asMillis(datetime));
    }

    public static final class Formats {
        public static final String AtYears = "yyyy";
        public static final String AtMonths = "yyyy-MM";
        public static final String AtDays = "yyyy-MM-dd";
        public static final String AtHours = "yyyy-MM-dd HH";
        public static final String AtMinutes = "yyyy-MM-dd HH:mm";
        public static final String AtSeconds = "yyyy-MM-dd HH:mm:ss";
    }

    public static final class Patterns {
        public static final DateTimeFormatter AtYears = DateTimeFormatter.ofPattern(Formats.AtYears);
        public static final DateTimeFormatter AtMonths = DateTimeFormatter.ofPattern(Formats.AtMonths);
        public static final DateTimeFormatter AtDays = DateTimeFormatter.ofPattern(Formats.AtDays);
        public static final DateTimeFormatter AtHours = DateTimeFormatter.ofPattern(Formats.AtHours);
        public static final DateTimeFormatter AtMinutes = DateTimeFormatter.ofPattern(Formats.AtMinutes);
        public static final DateTimeFormatter AtSeconds = DateTimeFormatter.ofPattern(Formats.AtSeconds);
    }
}

