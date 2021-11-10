package com.infilos.utils;

import java.time.Duration;

public final class Durations {
    private Durations(){
    }

    /**
     * 1d2h3s
     * Y: year
     * M: month
     * D: day
     * H: hour
     * M: minute
     * S: second
     *
     * PnYnMnDTnHnMnS
     */
    public static Duration parse(String string) {
        return Duration.parse("P".concat(string).replace("D","DT"));
    }
}
