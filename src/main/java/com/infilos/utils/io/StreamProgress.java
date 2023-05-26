package com.infilos.utils.io;

public interface StreamProgress {

    void start();

    void progress(long byteSize);

    void finish();
}
