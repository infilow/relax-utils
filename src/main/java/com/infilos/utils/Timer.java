package com.infilos.utils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 * Timer timer = Timer.init();
 * timer.span();
 * timer.span();
 * timer.stop();
 * String summary = timer.costSummary();
 * </pre>
 */
public final class Timer {
    private final String name;
    private long start = System.nanoTime();
    LinkedList<TimeSpan> spans = new LinkedList<>();

    private Timer(String name) {
        this.name = name;
    }

    public static Timer init() {
        return new Timer("");
    }

    public static Timer init(String name) {
        return new Timer(name);
    }

    public Timer start() {
        this.start = System.nanoTime();
        return this;
    }

    public TimeSpan stop(String tag) {
        long stop = System.nanoTime();
        TimeSpan span = new TimeSpan(tag, start, stop);
        spans.add(span);

        this.start = stop;

        return span;
    }

    public TimeSpan stop() {
        return stop("Span " + (spans.size() + 1));
    }

    public TimeSpan span(String tag) {
        return stop(tag);
    }

    public TimeSpan span() {
        return stop();
    }

    public String name() {
        return name;
    }

    public long costMillis() {
        return spans.stream().map(TimeSpan::costMillis).mapToLong(m -> m).sum();
    }

    public Duration costDuration() {
        return Duration.of(spans.stream().map(TimeSpan::costNanos).mapToLong(m -> m).sum(), ChronoUnit.NANOS);
    }

    public String costString() {
        return formatReadable(costMillis());
    }

    public String costSummary() {
        long totalMillis = costMillis();
        int maxSpanTagLength = spans.stream().map(s -> s.tag().length()).mapToInt(l -> l).max().orElse(0);

        StringBuilder builder = new StringBuilder(String.format(
            "%s include %s spans, cost %s:\n",
            name.isEmpty() ? "Timer" : name, spans.size(), formatReadable(totalMillis)));
        int totalLeft = 0;

        for (int idx = 0; idx < spans.size(); idx++) {
            TimeSpan span = spans.get(idx);
            int percent = (int) Math.round(span.costMillis() * 100D / totalMillis);

            // first
            if (idx == 0) {
                builder.append(padLeft(span.tag(), maxSpanTagLength, " "));
                builder.append("├").append(fillup("─", percent - 1)).append("┐");
                builder.append(percent).append("%: ").append(span.costString()).append("\n");
            }
            // last
            else if (idx == spans.size() - 1) {
                builder.append(padLeft(span.tag(), maxSpanTagLength, " "));
                builder.append(fillup(" ", totalLeft));
                builder.append("└").append(fillup("─", percent - 2)).append("┤");
                builder.append(percent).append("%: ").append(span.costString()).append("\n");
            }
            // middles
            else {
                builder.append(padLeft(span.tag(), maxSpanTagLength, " "));
                builder.append(fillup(" ", totalLeft));
                builder.append("└").append(fillup("─", percent - 1)).append("┐");
                builder.append(percent).append("%: ").append(span.costString()).append("\n");
            }

            totalLeft += percent;
        }

        return builder.toString();
    }

    static String formatReadable(long costMillis) {
        if (costMillis == 0L) {
            return "0 Millis";
        }

        long days = TimeUnit.MILLISECONDS.toDays(costMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(costMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(costMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(costMillis) % 60;
        long millis = costMillis % 1000;

        List<Long> costs = Arrays.asList(days, hours, minutes, seconds, millis);
        List<String> units = Arrays.asList("Days", "Hours", "Minutes", "Seconds", "Millis");

        int left = 0;
        int right = 5;

        for (int idx = 0; idx < 5; idx++) {
            if (costs.get(idx) == 0) {
                left = idx + 1;
            } else {
                break;
            }
        }
        for (int idx = 4; idx > -1; idx--) {
            if (costs.get(idx) == 0) {
                right = idx;
            } else {
                break;
            }
        }

        left = left == 5 ? 0 : left;

        costs = costs.subList(left, right);
        units = units.subList(left, right);

        if (costs.isEmpty()) {
            return "0 Millis";
        }

        List<String> segments = new ArrayList<>(costs.size());

        for (int idx = 0; idx < costs.size(); idx++) {
            segments.add(costs.get(idx) + " " + units.get(idx));
        }

        return String.join(" ", segments);
    }

    static String fillup(String value, int size) {
        return String.join("", new ArrayList<>(Collections.nCopies(size, value)));
    }

    static String padLeft(String input, int length, String value) {
        return (String.format("%" + length + "s", "").replace(" ", value) + input).substring(input.length(), length + input.length());
    }
}
