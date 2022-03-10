package com.infilos.retry;

public class CustomTestException extends RuntimeException {

    private final int someValue;

    public CustomTestException(String message, int someValue) {
        super(message);
        this.someValue = someValue;
    }

    public int getSomeValue() {
        return someValue;
    }
}
