package com.infilos.utils.io;

import com.infilos.utils.DataSize;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;

public class FileWrapper implements Serializable{
    private static final long serialVersionUID = 1L;

    protected File file;
    protected Charset charset;

    public FileWrapper(File file, Charset charset) {
        this.file = file;
        this.charset = charset;
    }

    public File getFile() {
        return file;
    }

    public FileWrapper setFile(File file) {
        this.file = file;
        return this;
    }

    public Charset getCharset() {
        return charset;
    }

    public FileWrapper setCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    public String readableFileSize() {
        return DataSize.of(file.length()).succinctDataSize().toString();
    }
}

