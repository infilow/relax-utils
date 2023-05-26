package com.infilos.utils.io;

import com.infilos.utils.Charsets;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FileAppender implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FileWriter writer;
    private final int capacity;
    private final boolean isNewLineMode;
    private final List<String> list = new ArrayList<>(100);

    public FileAppender(File destFile, int capacity, boolean isNewLineMode) {
        this(destFile, Charsets.UTF_8, capacity, isNewLineMode);
    }

    public FileAppender(File destFile, Charset charset, int capacity, boolean isNewLineMode) {
        this.capacity = capacity;
        this.isNewLineMode = isNewLineMode;
        this.writer = FileWriter.create(destFile, charset);
    }

    public FileAppender append(String line) {
        if (list.size() >= capacity) {
            flush();
        }
        list.add(line);
        return this;
    }

    public FileAppender flush() {
        try (PrintWriter pw = writer.getPrintWriter(true)) {
            for (String str : list) {
                pw.print(str);
                if (isNewLineMode) {
                    pw.println();
                }
            }
        }
        list.clear();
        return this;
    }
}
