package com.infilos.utils.retry;

import java.util.function.Consumer;

/**
 * Runnable with exception handler.
 */
@FunctionalInterface
interface CatchedRunnable {
    void run() throws Throwable;

    default void run(Consumer<? super Throwable> catchedHandler) {
        try {
            run();
        } catch (Throwable e) {
            catchedHandler.accept(e);
        }
    }
}
