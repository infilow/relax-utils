package com.infilos.retry;

/**
 * Status of each attempt.
 */
public class AttemptStatus<T> {

    private T result;

    private boolean hasSucced;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public boolean hasSucced() {
        return hasSucced;
    }

    public void setSucced(boolean alreadySucced) {
        this.hasSucced = alreadySucced;
    }
}
