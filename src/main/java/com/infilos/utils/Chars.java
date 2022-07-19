package com.infilos.utils;

@SuppressWarnings("unused")
public final class Chars {
    private Chars() {
    }

    private static final char SLASH = '/';
    private static final char BACKSLASH = '\\';
    private static final String[] CACHE = new String[128];

    static {
        for (char c = 0; c < 128; c++) {
            CACHE[c] = String.valueOf(c);
        }
    }

    public static boolean isAscii(char ch) {
        return ch < 128;
    }

    public static boolean isPrintableAscii(char ch) {
        return ch >= 32 && ch < 127;
    }

    public static boolean isControlAscii(final char ch) {
        return ch < 32 || ch == 127;
    }

    public static boolean isChinese(char ch) {
        return String.valueOf(ch).codePoints().anyMatch(codepoint ->
            Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
    }

    public static boolean isEnglish(char ch) {
        return isLetter(ch);
    }

    public static boolean isLetter(char ch) {
        return isUpperLetter(ch) || isLowerLetter(ch);
    }

    public static boolean isUpperLetter(final char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    public static boolean isLowerLetter(final char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    public static boolean isNumber(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isHexChar(char c) {
        return isNumber(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static boolean isLetterOrNumber(final char ch) {
        return isLetter(ch) || isNumber(ch);
    }

    private static String toString(char c) {
        return c < 128 ? CACHE[c] : String.valueOf(c);
    }

    public static boolean isCharClass(Class<?> clazz) {
        return clazz == Character.class || clazz == char.class;
    }

    public static boolean isChar(Object value) {
        // noinspection ConstantConditions
        return value instanceof Character || value.getClass() == char.class;
    }

    public static boolean isBlankChar(char c) {
        return isBlankChar((int) c);
    }

    public static boolean isBlankChar(int c) {
        return Character.isWhitespace(c)
            || Character.isSpaceChar(c)
            || c == '\ufeff'
            || c == '\u202a'
            || c == '\u0000';
    }

    public static boolean isEmoji(char c) {
        // noinspection ConstantConditions
        return !((c == 0x0) || //
            (c == 0x9) || //
            (c == 0xA) || //
            (c == 0xD) || //
            ((c >= 0x20) && (c <= 0xD7FF)) || //
            ((c >= 0xE000) && (c <= 0xFFFD)) || //
            ((c >= 0x100000) && (c <= 0x10FFFF)));
    }

    public static boolean isFileSeparator(char c) {
        return SLASH == c || BACKSLASH == c;
    }

    public static boolean equals(char c1, char c2, boolean caseInsensitive) {
        if (caseInsensitive) {
            return Character.toLowerCase(c1) == Character.toLowerCase(c2);
        }
        return c1 == c2;
    }

    public static int getType(int c) {
        return Character.getType(c);
    }

    public static int digit16(int b) {
        return Character.digit(b, 16);
    }

    public static char toCloseChar(char c) {
        int result = c;
        if (c >= '1' && c <= '9') {
            result = '①' + c - '1';
        } else if (c >= 'A' && c <= 'Z') {
            result = 'Ⓐ' + c - 'A';
        } else if (c >= 'a' && c <= 'z') {
            result = 'ⓐ' + c - 'a';
        }
        return (char) result;
    }

    public static char toCloseByNumber(int number) {
        if (number > 20) {
            throw new IllegalArgumentException("Number must be [1-20]");
        }
        return (char) ('①' + number - 1);
    }
}
