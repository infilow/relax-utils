package com.infilos.retry;

public interface RetryListener<T> {
    
    void onEvent(RetryStatus<T> status);
}
