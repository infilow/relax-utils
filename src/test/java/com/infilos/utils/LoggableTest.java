package com.infilos.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public class LoggableTest implements Loggable {

    @Test
    public void test() {
        assertEquals(log(),log());

        log().info("test log 0");

        System.out.println(log().getClass().getName());

        Loggable.switchRootLevel(Level.ERROR);
        log().info("test log 1");

        Loggable.switchRootLevel(Level.INFO);
        log().info("test log 2");   // available

        Loggable.switchLoggerLevel("com.infilos", Level.ERROR);
        log().info("test log 3");

        Loggable.switchLoggerLevel("com.infilos", Level.INFO);
        log().info("test log 4");   // available
        
        Loggable.loggers().forEach(logger -> System.out.println(logger.getName()));
    }
}