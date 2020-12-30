package com.infilos.utils.pool;

/**
 * @author zhiguang.zhang on 2020-03-26.
 */

public final class PoolClosedException extends RuntimeException {
    private PoolClosedException() {
    }

    public static PoolClosedException create() {
        return new PoolClosedException();
    }
}
