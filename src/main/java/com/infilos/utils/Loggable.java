package com.infilos.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public interface Loggable {

    /**
     * Return a 'cached' logger instance for concrete type.
     *
     * @return {@link Logger}
     */
    default Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Only support logger binding of {@link ch.qos.logback.classic.Logger}
     *
     * @param level is target logger level
     */
    static void switchRootLevel(Level level) {
        ((ch.qos.logback.classic.Logger)LoggerFactory
            .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)).setLevel(level.under);
    }

    /**
     * Only support logger binding of {@link ch.qos.logback.classic.Logger}
     *
     * @param name is target logger name
     * @param level is target logger level
     */
    static void switchLoggerLevel(String name, Level level) {
        ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(name)).setLevel(level.under);
    }

    enum Level {
        OFF(ch.qos.logback.classic.Level.OFF),
        ERROR(ch.qos.logback.classic.Level.ERROR),
        WARN(ch.qos.logback.classic.Level.WARN),
        INFO(ch.qos.logback.classic.Level.INFO),
        DEBUG(ch.qos.logback.classic.Level.DEBUG),
        TRACE(ch.qos.logback.classic.Level.TRACE),
        ALL(ch.qos.logback.classic.Level.ALL);

        private final ch.qos.logback.classic.Level under;

        Level(ch.qos.logback.classic.Level level) {
            this.under = level;
        }
    }

    static Logger logger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    static Logger logger(Class<?> clazz, String name) {
        return LoggerFactory.getLogger(clazz.getSimpleName() + "[" + name + "]");
    }
}
