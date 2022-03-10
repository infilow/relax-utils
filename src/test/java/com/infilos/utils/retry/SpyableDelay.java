package com.infilos.utils.retry;

import java.time.Duration;

/**
 * Looks like Mockito is having trouble to spy Delay.of(), which returns an anonymous class that happens to be final.
 */
class SpyableDelay<E> extends RetryDelay<E> {
    private final Duration duration;

    SpyableDelay(Duration duration) {
        this.duration = duration;
    }

    @Override
    public Duration duration() {
        return duration;
    }
}
