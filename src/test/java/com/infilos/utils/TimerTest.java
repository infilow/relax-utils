package com.infilos.utils;

import org.junit.Test;

public class TimerTest {

    @Test
    public void readable() {
        System.out.println(Timer.formatReadable(24*60*60*1000L));
        System.out.println(Timer.formatReadable(23*60*60*1000L));
        System.out.println(Timer.formatReadable(59*60*1000L));
        System.out.println(Timer.formatReadable(59*1000L));
        System.out.println(Timer.formatReadable(900L));
        System.out.println(Timer.formatReadable(0L));

        System.out.println(Timer.formatReadable(24 * 60 * 60 * 1000L + 60 * 60 * 1000L));
        System.out.println(Timer.formatReadable(24 * 60 * 60 * 1000L + 60 * 60 * 1000L + 60 * 1000L));
        System.out.println(Timer.formatReadable(24 * 60 * 60 * 1000L + 60 * 60 * 1000L + 60 * 1000L + 1000L));
        System.out.println(Timer.formatReadable(24 * 60 * 60 * 1000L + 60 * 60 * 1000L + 60 * 1000L + 1000L + 200L));

        System.out.println(Timer.formatReadable(60 * 60 * 1000L + 60 * 1000L + 1000L));
        System.out.println(Timer.formatReadable(60 * 60 * 1000L));
        System.out.println(Timer.formatReadable(60 * 1000L + 1000L));
        System.out.println(Timer.formatReadable(60 * 1000L + 300L));
        System.out.println(Timer.formatReadable(3100L));
    }
    
    @Test
    public void summary() {
        Timer timer = Timer.init();
        timer.spans.add(new TimeSpan("S1", 100 * 1000000, 400 * 1000000));
        timer.spans.add(new TimeSpan("SS2", 100 * 1000000, 1200 * 1000000L));
        timer.spans.add(new TimeSpan("SSSS3", 100 * 1000000, 800 * 1000000));

        System.out.println(timer.costSummary());
    }
    
    @Test
    public void fillAndPad() {
        System.out.println(String.join("", Timer.fillup("A",10)));
        System.out.println(Timer.padLeft("AA", 10, "_"));
    }
}