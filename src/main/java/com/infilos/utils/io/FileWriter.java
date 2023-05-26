package com.infilos.utils.io;

import com.infilos.utils.Charsets;
import com.infilos.utils.FileHelper;
import com.infilos.utils.IOStreams;
import com.infilos.utils.Require;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class FileWriter extends FileWrapper {
    private static final long serialVersionUID = 1L;

    public static FileWriter create(File file, Charset charset) {
        return new FileWriter(file, charset);
    }

    public static FileWriter create(File file) {
        return new FileWriter(file);
    }

    public FileWriter(File file, Charset charset) {
        super(file, charset);
        checkFile();
    }

    public FileWriter(File file, String charset) {
        this(file, Charsets.charset(charset));
    }

    public FileWriter(String filePath, Charset charset) {
        this(FileHelper.of(filePath), charset);
    }

    public FileWriter(String filePath, String charset) {
        this(FileHelper.of(filePath), Charsets.charset(charset));
    }

    public FileWriter(File file) {
        this(file, Charsets.UTF_8);
    }

    public FileWriter(String filePath) {
        this(filePath, Charsets.UTF_8);
    }

    public File write(String content, boolean isAppend) throws IORuntimeException {
        BufferedWriter writer = null;
        try {
            writer = getWriter(isAppend);
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            IOStreams.closeQuietly(writer);
        }
        return file;
    }

    public File write(String content) throws IORuntimeException {
        return write(content, false);
    }

    public File append(String content) throws IORuntimeException {
        return write(content, true);
    }

    public <T> File writeLines(Collection<T> list) throws IORuntimeException {
        return writeLines(list, false);
    }

    public <T> File appendLines(Collection<T> list) throws IORuntimeException {
        return writeLines(list, true);
    }

    public <T> File writeLines(Collection<T> list, boolean isAppend) throws IORuntimeException {
        return writeLines(list, null, isAppend);
    }

    public <T> File writeLines(Collection<T> list, LineSeparator lineSeparator, boolean isAppend) throws IORuntimeException {
        try (PrintWriter writer = getPrintWriter(isAppend)) {
            for (T t : list) {
                if (null != t) {
                    writer.print(t);
                    printNewLine(writer, lineSeparator);
                    writer.flush();
                }
            }
        }
        return this.file;
    }

    public File writeMap(Map<?, ?> map, String kvSeparator, boolean isAppend) throws IORuntimeException {
        return writeMap(map, null, kvSeparator, isAppend);
    }

    public File writeMap(Map<?, ?> map, LineSeparator lineSeparator, String kvSeparator, boolean isAppend) throws IORuntimeException {
        if (null == kvSeparator) {
            kvSeparator = " = ";
        }
        try (PrintWriter writer = getPrintWriter(isAppend)) {
            for (Entry<?, ?> entry : map.entrySet()) {
                if (null != entry) {
                    writer.print(String.format("%s%s%s", entry.getKey(), kvSeparator, entry.getValue()));
                    printNewLine(writer, lineSeparator);
                    writer.flush();
                }
            }
        }
        return this.file;
    }

    public File write(byte[] data, int off, int len) throws IORuntimeException {
        return write(data, off, len, false);
    }

    public File append(byte[] data, int off, int len) throws IORuntimeException {
        return write(data, off, len, true);
    }

    public File write(byte[] data, int off, int len, boolean isAppend) throws IORuntimeException {
        try (FileOutputStream out = new FileOutputStream(FileHelper.touch(file), isAppend)) {
            out.write(data, off, len);
            out.flush();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return file;
    }

    public File writeFromStream(InputStream in) throws IORuntimeException {
        return writeFromStream(in, true);
    }

    public File writeFromStream(InputStream in, boolean isCloseIn) throws IORuntimeException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(FileHelper.touch(file));
            IOStreams.copy(in, out);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            IOStreams.closeQuietly(out);
            if (isCloseIn) {
                IOStreams.closeQuietly(in);
            }
        }
        return file;
    }

    public BufferedOutputStream getOutputStream() throws IORuntimeException {
        try {
            return new BufferedOutputStream(Files.newOutputStream(FileHelper.touch(file).toPath()));
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public BufferedWriter getWriter(boolean isAppend) throws IORuntimeException {
        try {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileHelper.touch(file), isAppend), charset));
        } catch (Exception e) {
            throw new IORuntimeException(e);
        }
    }

    public PrintWriter getPrintWriter(boolean isAppend) throws IORuntimeException {
        return new PrintWriter(getWriter(isAppend));
    }

    private void checkFile() throws IORuntimeException {
        Require.checkNotNull(file, "File to write content is null !");

        if (this.file.exists() && !file.isFile()) {
            throw new IORuntimeException("File [{}] is not a file !", this.file.getAbsoluteFile());
        }
    }

    private void printNewLine(PrintWriter writer, LineSeparator lineSeparator) {
        if (null == lineSeparator) {
            writer.println();
        } else {
            writer.print(lineSeparator.getValue());
        }
    }
}
