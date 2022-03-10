package com.infilos.api;

@FunctionalInterface
public interface CheckedRunnable<E extends Throwable> {
    void run() throws E;
}
