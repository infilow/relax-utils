package com.infilos.utils;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public class SchedulerTest {
    static {
        Loggable.switchLoggerLevel("com.infilos", Loggable.Level.DEBUG);
    }

    private static final Scheduler scheduler = Scheduler.create(2).startup();

    @Test
    public void testOnce() {
        scheduler.scheduleOnce("AAA", () -> System.out.println("AAA"), 5000);
        scheduler.scheduleOnce("BBB", () -> System.out.println("BBB"), 6, TimeUnit.SECONDS);

        Threads.keep();
    }

    @Test
    public void testRepeat() {
        scheduler.schedule("AAA", () -> System.out.println("AAA"), 2, 4, TimeUnit.SECONDS);
        scheduler.schedule("BBB", () -> System.out.println("BBB"), 2, 6, TimeUnit.SECONDS);

        Threads.keep();
    }
}