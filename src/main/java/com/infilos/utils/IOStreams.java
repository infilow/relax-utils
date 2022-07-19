package com.infilos.utils;

import com.infilos.utils.io.FastByteArrayOutputStream;
import com.infilos.utils.io.IORuntimeException;
import com.infilos.utils.io.StreamProgress;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public final class IOStreams {
    private IOStreams() {
    }

    private static final int STREAM_EOF = -1;
    private static final int BUFFER_SIZE = 2048;

    public static long copy(Reader reader, Writer writer) throws IORuntimeException {
        return copy(reader, writer, BUFFER_SIZE);
    }

    public static long copy(Reader reader, Writer writer, int bufferSize) throws IORuntimeException {
        return copy(reader, writer, bufferSize, null);
    }

    public static long copy(Reader reader, Writer writer, int bufferSize, StreamProgress streamProgress) throws IORuntimeException {
        char[] buffer = new char[bufferSize];
        long size = 0;
        int readSize;

        if (null != streamProgress) {
            streamProgress.start();
        }
        try {
            while ((readSize = reader.read(buffer, 0, bufferSize)) != STREAM_EOF) {
                writer.write(buffer, 0, readSize);
                size += readSize;
                writer.flush();
                if (null != streamProgress) {
                    streamProgress.progress(size);
                }
            }
        } catch (Exception e) {
            throw new IORuntimeException(e);
        }
        if (null != streamProgress) {
            streamProgress.finish();
        }
        return size;
    }

    public static long copy(InputStream in, OutputStream out) throws IORuntimeException {
        return copy(in, out, BUFFER_SIZE);
    }

    public static long copy(InputStream in, OutputStream out, int bufferSize) throws IORuntimeException {
        return copy(in, out, bufferSize, null);
    }

    public static long copy(InputStream in, OutputStream out, int bufferSize, StreamProgress streamProgress) throws IORuntimeException {
        Require.checkNotNull(in, "InputStream is null !");
        Require.checkNotNull(out, "OutputStream is null !");

        if (bufferSize <= 0) {
            bufferSize = BUFFER_SIZE;
        }

        byte[] buffer = new byte[bufferSize];
        long size = 0;

        if (null != streamProgress) {
            streamProgress.start();
        }
        try {
            for (int readSize; (readSize = in.read(buffer)) != STREAM_EOF; ) {
                out.write(buffer, 0, readSize);
                size += readSize;
                out.flush();
                if (null != streamProgress) {
                    streamProgress.progress(size);
                }
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        if (null != streamProgress) {
            streamProgress.finish();
        }

        return size;
    }

    public static long copyWithNio(InputStream in, OutputStream out, int bufferSize, StreamProgress streamProgress) throws IORuntimeException {
        return copy(Channels.newChannel(in), Channels.newChannel(out), bufferSize, streamProgress);
    }

    public static long copy(FileInputStream in, FileOutputStream out) throws IORuntimeException {
        Require.checkNotNull(in, "FileInputStream is null!");
        Require.checkNotNull(out, "FileOutputStream is null!");

        final FileChannel inChannel = in.getChannel();
        final FileChannel outChannel = out.getChannel();

        try {
            return inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static long copy(ReadableByteChannel in, WritableByteChannel out, int bufferSize, StreamProgress streamProgress) throws IORuntimeException {
        Require.checkNotNull(in, "InputStream is null !");
        Require.checkNotNull(out, "OutputStream is null !");

        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize <= 0 ? BUFFER_SIZE : bufferSize);
        long size = 0;

        if (null != streamProgress) {
            streamProgress.start();
        }
        try {
            while (in.read(byteBuffer) != STREAM_EOF) {
                byteBuffer.flip();
                size += out.write(byteBuffer);
                byteBuffer.clear();
                if (null != streamProgress) {
                    streamProgress.progress(size);
                }
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        if (null != streamProgress) {
            streamProgress.finish();
        }

        return size;
    }

    public static BufferedReader createReader(InputStream in, String charsetName) {
        return createReader(in, Charset.forName(charsetName));
    }

    public static BufferedReader createReader(InputStream in, Charset charset) {
        if (null == in) {
            return null;
        }

        InputStreamReader reader;
        if (null == charset) {
            reader = new InputStreamReader(in);
        } else {
            reader = new InputStreamReader(in, charset);
        }

        return new BufferedReader(reader);
    }

    public static BufferedReader createReader(Reader reader) {
        if (null == reader) {
            return null;
        }

        return (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);
    }

    public static PushbackReader createPushBackReader(Reader reader, int pushBackSize) {
        return (reader instanceof PushbackReader) ? (PushbackReader) reader : new PushbackReader(reader, pushBackSize);
    }

    public static OutputStreamWriter createWriter(OutputStream out, String charsetName) {
        return createWriter(out, Charset.forName(charsetName));
    }

    public static OutputStreamWriter createWriter(OutputStream out, Charset charset) {
        if (null == out) {
            return null;
        }

        if (null == charset) {
            return new OutputStreamWriter(out);
        } else {
            return new OutputStreamWriter(out, charset);
        }
    }

    public static String read(InputStream in, String charsetName) throws IORuntimeException {
        FastByteArrayOutputStream out = read(in);
        return Strings.isBlank(charsetName) ? out.toString() : out.toString(charsetName);
    }

    public static String read(InputStream in, Charset charset) throws IORuntimeException {
        FastByteArrayOutputStream out = read(in);
        return null == charset ? out.toString() : out.toString(charset);
    }

    public static FastByteArrayOutputStream read(InputStream in) throws IORuntimeException {
        final FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        copy(in, out);
        return out;
    }

    public static String read(Reader reader) throws IORuntimeException {
        final StringBuilder builder = new StringBuilder();
        final CharBuffer buffer = CharBuffer.allocate(BUFFER_SIZE);
        try {
            while (-1 != reader.read(buffer)) {
                builder.append(buffer.flip());
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }

        return builder.toString();
    }

    public static String read(FileChannel fileChannel) throws IORuntimeException {
        return read(fileChannel, StandardCharsets.UTF_8);
    }

    public static String read(FileChannel fileChannel, String charsetName) throws IORuntimeException {
        return read(fileChannel, Charset.forName(charsetName));
    }

    public static String read(FileChannel fileChannel, Charset charset) throws IORuntimeException {
        MappedByteBuffer buffer;
        try {
            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).load();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return Strings.of(buffer, charset);
    }

    public static byte[] readBytes(InputStream in) throws IORuntimeException {
        final FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static byte[] readBytes(InputStream in, int length) throws IORuntimeException {
        if (null == in) {
            return null;
        }
        if (length <= 0) {
            return new byte[0];
        }

        byte[] b = new byte[length];
        int readLength;
        try {
            readLength = in.read(b);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        if (readLength < length) {
            byte[] b2 = new byte[length];
            System.arraycopy(b, 0, b2, 0, readLength);
            return b2;
        } else {
            return b;
        }
    }

    public static <T> T readObject(InputStream in) throws IORuntimeException {
        Require.checkNotNull(in, "The InputStream must not be null");

        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(in);
            // may fail with CCE if serialised form is incorrect
            @SuppressWarnings("unchecked") final T obj = (T) ois.readObject();

            return obj;
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends Collection<String>> T readLines(InputStream in, T collection) throws IORuntimeException {
        return readLines(in, StandardCharsets.UTF_8, collection);
    }

    public static <T extends Collection<String>> T readLines(InputStream in, String charsetName, T collection) throws IORuntimeException {
        return readLines(in, Charset.forName(charsetName), collection);
    }

    public static <T extends Collection<String>> T readLines(InputStream in, Charset charset, T collection) throws IORuntimeException {
        return readLines(createReader(in, charset), collection);
    }

    public static <T extends Collection<String>> T readLines(Reader reader, final T collection) throws IORuntimeException {
        readLines(reader, (Consumer<String>) collection::add);
        return collection;
    }

    public static void readLines(InputStream in, Consumer<String> consumer) throws IORuntimeException {
        readLines(in, StandardCharsets.UTF_8, consumer);
    }

    public static void readLines(InputStream in, Charset charset, Consumer<String> consumer) throws IORuntimeException {
        readLines(createReader(in, charset), consumer);
    }

    public static void readLines(Reader reader, Consumer<String> consumer) throws IORuntimeException {
        Require.checkNotNull(reader);
        Require.checkNotNull(consumer);

        final BufferedReader bReader = createReader(reader);
        String line;
        try {
            while ((line = bReader.readLine()) != null) {
                consumer.accept(line);
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static ByteArrayInputStream createStream(String content, String charsetName) {
        return createStream(content, Charset.forName(charsetName));
    }

    public static ByteArrayInputStream createStream(String content, Charset charset) {
        if (content == null) {
            return null;
        }
        return new ByteArrayInputStream(content.getBytes(charset));
    }

    public static FileInputStream createStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new IORuntimeException(e);
        }
    }

    public static PushbackInputStream createPushbackStream(InputStream in, int pushBackSize) {
        return (in instanceof PushbackInputStream) ? (PushbackInputStream) in : new PushbackInputStream(in, pushBackSize);
    }

    public static BufferedInputStream createBuffered(InputStream in) {
        Require.checkNotNull(in, "InputStream must be not null!");
        return (in instanceof BufferedInputStream) ? (BufferedInputStream) in : new BufferedInputStream(in);
    }

    public static BufferedInputStream createBuffered(InputStream in, int bufferSize) {
        Require.checkNotNull(in, "InputStream must be not null!");
        return (in instanceof BufferedInputStream) ? (BufferedInputStream) in : new BufferedInputStream(in, bufferSize);
    }

    public static BufferedOutputStream createBuffered(OutputStream out) {
        Require.checkNotNull(out, "OutputStream must be not null!");
        return (out instanceof BufferedOutputStream) ? (BufferedOutputStream) out : new BufferedOutputStream(out);
    }

    public static BufferedOutputStream createBuffered(OutputStream out, int bufferSize) {
        Require.checkNotNull(out, "OutputStream must be not null!");
        return (out instanceof BufferedOutputStream) ? (BufferedOutputStream) out : new BufferedOutputStream(out, bufferSize);
    }

    public static BufferedReader createBuffered(Reader reader) {
        Require.checkNotNull(reader, "Reader must be not null!");
        return (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);
    }

    public static BufferedReader createBuffered(Reader reader, int bufferSize) {
        Require.checkNotNull(reader, "Reader must be not null!");
        return (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader, bufferSize);
    }

    public static BufferedWriter createBuffered(Writer writer) {
        Require.checkNotNull(writer, "Writer must be not null!");
        return (writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer);
    }

    public static BufferedWriter createBuffered(Writer writer, int bufferSize) {
        Require.checkNotNull(writer, "Writer must be not null!");
        return (writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer, bufferSize);
    }

    public static void write(OutputStream out, boolean needClose, byte[] content) throws IOException {
        try {
            out.write(content);
        } finally {
            if (needClose) {
                closeQuietly(out);
            }
        }
    }

    public static void write(OutputStream out, boolean needClose, Object... contents) throws IOException {
        write(out, StandardCharsets.UTF_8, needClose, contents);
    }

    public static void write(OutputStream out, String charsetName, boolean needClose, Object... contents) throws IOException {
        write(out, Charset.forName(charsetName), needClose, contents);
    }

    public static void write(OutputStream out, Charset charset, boolean needClose, Object... contents) throws IOException {
        OutputStreamWriter osw = null;
        try {
            osw = createWriter(out, charset);
            for (Object content : contents) {
                if (content != null) {
                    osw.write(content.toString());
                    osw.flush();
                }
            }
        } finally {
            if (needClose) {
                closeQuietly(osw);
            }
        }
    }

    public static void write(OutputStream out, boolean needClose, Serializable... contents) throws IOException {
        ObjectOutputStream osw = null;
        try {
            osw = out instanceof ObjectOutputStream ? (ObjectOutputStream) out : new ObjectOutputStream(out);
            for (Object content : contents) {
                if (content != null) {
                    osw.writeObject(content);
                    osw.flush();
                }
            }
        } finally {
            if (needClose) {
                closeQuietly(osw);
            }
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }
}
