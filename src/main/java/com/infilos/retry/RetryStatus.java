package com.infilos.retry;

import java.time.Duration;

/**
 * Final result after all attempts.
 */
public class RetryStatus<T> extends AttemptStatus<T> {
    private String id;
    private long startTime;
    private long finishTime;
    private String operation;
    private int totalTries;
    private Duration totalDuration;
    private Exception lastException;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public int getTotalTries() {
        return totalTries;
    }

    public void setTotalTries(int totalTries) {
        this.totalTries = totalTries;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Duration totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Exception getLastException() {
        return lastException;
    }

    public void setLastException(Exception lastException) {
        this.lastException = lastException;
    }

    @Override
    public String toString() {
        return "RetryResult{" +
            "id='" + id + '\'' +
            ", startTime=" + startTime +
            ", finishTime=" + finishTime +
            ", operation='" + operation + '\'' +
            ", totalTries=" + totalTries +
            ", totalDuration=" + totalDuration +
            ", lastException=" + lastException +
            '}';
    }
}
