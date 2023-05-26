package com.infilos.utils.io;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class FastByteArrayOutputStream extends OutputStream {

    private final FastByteBuffer buffer;

    public FastByteArrayOutputStream() {
        this(1024);
    }

    public FastByteArrayOutputStream(int size) {
        buffer = new FastByteBuffer(size);
    }

    @Override
    public void write(@Nonnull byte[] b, int off, int len) {
        buffer.append(b, off, len);
    }

    @Override
    public void write(int b) {
        buffer.append((byte) b);
    }

    public int size() {
        return buffer.size();
    }

    @Override
    public void close() {
        // noop
    }

    public void reset() {
        buffer.reset();
    }

    public void writeTo(OutputStream out) throws IORuntimeException {
        final int index = buffer.index();
        byte[] buf;
        try {
            for (int i = 0; i < index; i++) {
                buf = buffer.array(i);
                out.write(buf);
            }
            out.write(buffer.array(index), 0, buffer.offset());
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public byte[] toByteArray() {
        return buffer.toArray();
    }

    @Override
    public String toString() {
        return new String(toByteArray());
    }

    public String toString(String charsetName) {
        return toString(Charset.forName(charsetName));
    }

    public String toString(Charset charset) {
        return new String(toByteArray(), charset);
    }

}
