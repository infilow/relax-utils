package com.infilos.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public class ShutdownTest {

    @Test
    public void test() {
        Shutdown.setup(1, () -> System.out.println("hook 1"));
        Shutdown.setup(2, () -> System.out.println("hook 2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void conflict() {
        Shutdown.setup(3, () -> System.out.println("hook 3"));
        Shutdown.setup(3, () -> System.out.println("hook 3"));
    }
}