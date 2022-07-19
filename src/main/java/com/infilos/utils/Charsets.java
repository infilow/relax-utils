package com.infilos.utils;

import com.infilos.utils.io.IORuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.*;

/**
 * <pre>
 * Other charset encoding/detecting tools:
 * - https://github.com/unicode-org/icu
 * - https://github.com/albfernandez/juniversalchardet
 * </pre>
 */
@SuppressWarnings("unused")
public final class Charsets {
    private Charsets() {
    }

    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
    public static final Charset GBK;
    private static final Charset[] DETECT_CHARSETS;

    static {
        // for OS which not support GBK
        Charset _GBK = null;
        try {
            _GBK = Charset.forName("GBK");
        } catch (UnsupportedCharsetException ignore) {
        }
        GBK = _GBK;

        DETECT_CHARSETS = new Charset[]{
            UTF_8,
            GBK,
            Charset.forName("GB2312"),
            Charset.forName("GB18030"),
            StandardCharsets.UTF_16BE,
            StandardCharsets.UTF_16LE,
            StandardCharsets.UTF_16,
            Charset.forName("BIG5"),
            Charset.forName("UNICODE"),
            StandardCharsets.US_ASCII
        };
    }

    public static Charset charset(String charsetName) throws UnsupportedCharsetException {
        return Strings.isBlank(charsetName) ? Charset.defaultCharset() : Charset.forName(charsetName);
    }

    public static Charset parse(String charsetName) {
        return parse(charsetName, Charset.defaultCharset());
    }

    public static Charset parse(String charsetName, Charset defaultCharset) {
        if (Strings.isBlank(charsetName)) {
            return defaultCharset;
        }

        Charset result;
        try {
            result = Charset.forName(charsetName);
        } catch (UnsupportedCharsetException e) {
            result = defaultCharset;
        }

        return result;
    }

    public static String convert(String source, String srcCharset, String destCharset) {
        return convert(source, Charset.forName(srcCharset), Charset.forName(destCharset));
    }

    public static String convert(String source, Charset srcCharset, Charset destCharset) {
        if (null == srcCharset) {
            srcCharset = StandardCharsets.ISO_8859_1;
        }

        if (null == destCharset) {
            destCharset = StandardCharsets.UTF_8;
        }

        if (Strings.isBlank(source) || srcCharset.equals(destCharset)) {
            return source;
        }
        return new String(source.getBytes(srcCharset), destCharset);
    }

    public static File convert(File file, Charset srcCharset, Charset destCharset) {
        final String str = FileHelper.readString(file, srcCharset);
        return FileHelper.writeString(str, file, destCharset);
    }

    public static String systemCharsetName() {
        return systemCharset().name();
    }

    public static Charset systemCharset() {
        return defaultCharset();
    }

    public static String defaultCharsetName() {
        return defaultCharset().name();
    }

    public static Charset defaultCharset() {
        return Charset.defaultCharset();
    }

    public static Charset detect(File file, Charset... charsets) {
        return detect(PathHelper.getInputStream(file.toPath()), charsets);
    }

    public static Charset detect(InputStream in, Charset... charsets) {
        return detect(4096, in, charsets);
    }

    public static Charset detect(int bufferSize, InputStream in, Charset... charsets) {
        if (Arrays.isEmpty(charsets)) {
            charsets = DETECT_CHARSETS;
        }

        final byte[] buffer = new byte[bufferSize];
        try {
            while (in.read(buffer) > -1) {
                for (Charset charset : charsets) {
                    final CharsetDecoder decoder = charset.newDecoder();
                    if (decodeBytes(buffer, decoder)) {
                        return charset;
                    }
                }
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            IOStreams.closeQuietly(in);
        }
        return null;
    }

    private static boolean decodeBytes(byte[] bytes, CharsetDecoder decoder) {
        try {
            decoder.decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            return false;
        }

        return true;
    }
}
