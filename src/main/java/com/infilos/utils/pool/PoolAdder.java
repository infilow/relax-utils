package com.infilos.utils.pool;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author zhiguang.zhang on 2020-03-26.
 */

public final class PoolAdder extends LongAdder {
    private PoolAdder() {
    }

    public long count() {
        return this.sum();
    }

    public static PoolAdder create() {
        return new PoolAdder();
    }
}

