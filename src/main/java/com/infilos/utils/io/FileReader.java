package com.infilos.utils.io;

import com.infilos.utils.Charsets;
import com.infilos.utils.FileHelper;
import com.infilos.utils.IOStreams;
import com.infilos.utils.PathHelper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class FileReader extends FileWrapper {
    private static final long serialVersionUID = 1L;

    public static FileReader create(File file, Charset charset) {
        return new FileReader(file, charset);
    }

    public static FileReader create(File file) {
        return new FileReader(file);
    }

    public FileReader(File file, Charset charset) {
        super(file, charset);
        checkFile();
    }

    public FileReader(File file, String charset) {
        this(file, Charsets.charset(charset));
    }

    public FileReader(String filePath, Charset charset) {
        this(FileHelper.of(filePath), charset);
    }

    public FileReader(String filePath, String charset) {
        this(FileHelper.of(filePath), Charsets.charset(charset));
    }

    public FileReader(File file) {
        this(file, Charsets.UTF_8);
    }

    public FileReader(String filePath) {
        this(filePath, Charsets.UTF_8);
    }

    public byte[] readBytes() throws IORuntimeException {
        long len = file.length();
        if (len >= Integer.MAX_VALUE) {
            throw new IORuntimeException("File is larger then max array size");
        }

        byte[] bytes = new byte[(int) len];
        FileInputStream in = null;
        int readLength;
        try {
            in = new FileInputStream(file);
            readLength = in.read(bytes);
            if (readLength < len) {
                throw new IORuntimeException(String.format("File length is [%s] but read [%s]!", len, readLength));
            }
        } catch (Exception e) {
            throw new IORuntimeException(e);
        } finally {
            IOStreams.closeQuietly(in);
        }

        return bytes;
    }

    public String readString() throws IORuntimeException {
        return new String(readBytes(), this.charset);
    }

    public <T extends Collection<String>> T readLines(T collection) throws IORuntimeException {
        BufferedReader reader = null;
        try {
            reader = PathHelper.getReader(file.toPath(), charset);
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                collection.add(line);
            }
            return collection;
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            IOStreams.closeQuietly(reader);
        }
    }

    public void readLines(Consumer<String> lineHandler) throws IOException {
        BufferedReader reader = null;
        try {
            reader = PathHelper.getReader(file.toPath(), charset);
            IOStreams.readLines(reader, lineHandler);
        } finally {
            IOStreams.closeQuietly(reader);
        }
    }

    public List<String> readLines() throws IORuntimeException {
        return readLines(new ArrayList<>());
    }

    public <T> T read(ReaderHandler<T> readerHandler) throws IORuntimeException {
        BufferedReader reader = null;
        T result;
        try {
            reader = PathHelper.getReader(this.file.toPath(), charset);
            result = readerHandler.handle(reader);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            IOStreams.closeQuietly(reader);
        }
        return result;
    }

    public BufferedReader getReader() throws IORuntimeException {
        return IOStreams.createReader(getInputStream(), this.charset);
    }

    public BufferedInputStream getInputStream() throws IORuntimeException {
        try {
            return new BufferedInputStream(Files.newInputStream(this.file.toPath()));
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public long writeToStream(OutputStream out) throws IORuntimeException {
        return writeToStream(out, false);
    }

    public long writeToStream(OutputStream out, boolean isCloseOut) throws IORuntimeException {
        try (FileInputStream in = new FileInputStream(this.file)) {
            return IOStreams.copy(in, out);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            if (isCloseOut) {
                IOStreams.closeQuietly(out);
            }
        }
    }

    private void checkFile() throws IORuntimeException {
        if (!file.exists()) {
            throw new IORuntimeException("File not exist: " + file);
        }
        if (!file.isFile()) {
            throw new IORuntimeException("Not a file:" + file);
        }
    }

    public interface ReaderHandler<T> {
        T handle(BufferedReader reader) throws IOException;
    }
}