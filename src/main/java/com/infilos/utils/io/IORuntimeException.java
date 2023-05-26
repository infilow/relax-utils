package com.infilos.utils.io;

import com.infilos.utils.Throws;

public class IORuntimeException extends RuntimeException {
    
    public IORuntimeException(Throwable e) {
        super(Throws.getMessage(e), e);
    }

    public IORuntimeException(String message) {
        super(message);
    }

    public IORuntimeException(String template, Object... params) {
        super(String.format(template, params));
    }

    public IORuntimeException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public IORuntimeException(Throwable throwable, String template, Object... params) {
        super(String.format(template, params), throwable);
    }

    public boolean isCauseInstanceOf(Class<? extends Throwable> clazz) {
        return clazz.isInstance(this.getCause());
    }
}
