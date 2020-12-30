package com.infilos.utils;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public final class Threads {
    private Threads() {
    }

    public static void keep() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {
        }
    }

    public static void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ignore) {
        }
    }
}