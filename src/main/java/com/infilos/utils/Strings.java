package com.infilos.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class Strings {
    private Strings() {
    }

    private static final String EMPTY = "";
    private static final String UNDERLINE = "_";

    public static boolean isBlank(CharSequence str) {
        int length;

        if ((str == null) || ((length = str.length()) == 0)) {
            return true;
        }

        for (int i = 0; i < length; i++) {
            if (!Numbers.isBlankChar(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }

    public static boolean containsBlank(CharSequence... strs) {
        if (Arrays.isEmpty(strs)) {
            return true;
        }

        for (CharSequence str : strs) {
            if (isBlank(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllBlank(CharSequence... strs) {
        if (Arrays.isEmpty(strs)) {
            return true;
        }

        for (CharSequence str : strs) {
            if (isNotBlank(str)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    public static String nullOrEmpty(CharSequence str) {
        return nullOrDefault(str, EMPTY);
    }

    public static String nullOrDefault(CharSequence str, String defaultStr) {
        return (str == null) ? defaultStr : str.toString();
    }

    public static String emptyOrNull(CharSequence str) {
        return isEmpty(str) ? null : str.toString();
    }

    public static boolean containsEmpty(CharSequence... strs) {
        if (Arrays.isEmpty(strs)) {
            return true;
        }

        for (CharSequence str : strs) {
            if (isEmpty(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllEmpty(CharSequence... strs) {
        if (Arrays.isEmpty(strs)) {
            return true;
        }

        for (CharSequence str : strs) {
            if (isNotEmpty(str)) {
                return false;
            }
        }
        return true;
    }

    public static String trim(CharSequence str) {
        return (null == str) ? null : trim(str, 0);
    }

    public static void trim(String[] strs) {
        if (null == strs) {
            return;
        }
        String str;
        for (int i = 0; i < strs.length; i++) {
            str = strs[i];
            if (null != str) {
                strs[i] = str.trim();
            }
        }
    }

    public static String trimToEmpty(CharSequence str) {
        return str == null ? EMPTY : trim(str, 0);
    }

    public static String trimStart(CharSequence str) {
        return trim(str, -1);
    }

    public static String trimEnd(CharSequence str) {
        return trim(str, 1);
    }

    public static String trim(CharSequence str, int mode) {
        if (str == null) {
            return null;
        }

        int length = str.length();
        int start = 0;
        int end = length;

        // start
        if (mode <= 0) {
            while ((start < end) && (Numbers.isBlankChar(str.charAt(start)))) {
                start++;
            }
        }

        // end
        if (mode >= 0) {
            while ((start < end) && (Numbers.isBlankChar(str.charAt(end - 1)))) {
                end--;
            }
        }

        if ((start > 0) || (end < length)) {
            return str.toString().substring(start, end);
        }

        return str.toString();
    }

    public static boolean startWith(CharSequence str, char c) {
        return c == str.charAt(0);
    }

    public static boolean startWith(CharSequence str, CharSequence prefix, boolean isIgnoreCase) {
        if (isIgnoreCase) {
            return str.toString().toLowerCase().startsWith(prefix.toString().toLowerCase());
        } else {
            return str.toString().startsWith(prefix.toString());
        }
    }

    public static boolean startWith(CharSequence str, CharSequence prefix) {
        return startWith(str, prefix, false);
    }

    public static boolean startWithIgnoreCase(String str, String prefix) {
        return startWith(str, prefix, true);
    }

    public static boolean startWithAny(CharSequence str, CharSequence... prefixes) {
        if (isEmpty(str) || Arrays.isEmpty(prefixes)) {
            return false;
        }

        for (CharSequence suffix : prefixes) {
            if (startWith(str, suffix, false)) {
                return true;
            }
        }
        return false;
    }

    public static boolean endWith(CharSequence str, char c) {
        return c == str.charAt(str.length() - 1);
    }

    public static boolean endWith(CharSequence str, CharSequence suffix, boolean isIgnoreCase) {
        if (isBlank(str) || isBlank(suffix)) {
            return false;
        }

        if (isIgnoreCase) {
            return str.toString().toLowerCase().endsWith(suffix.toString().toLowerCase());
        } else {
            return str.toString().endsWith(suffix.toString());
        }
    }

    public static boolean endWith(CharSequence str, CharSequence suffix) {
        return endWith(str, suffix, false);
    }

    public static boolean endWithIgnoreCase(CharSequence str, String suffix) {
        return endWith(str, suffix, true);
    }

    public static boolean endWithAny(CharSequence str, CharSequence... suffixes) {
        if (isEmpty(str) || Arrays.isEmpty(suffixes)) {
            return false;
        }

        for (CharSequence suffix : suffixes) {
            if (endWith(str, suffix, false)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsIgnoreCase(CharSequence str, CharSequence testStr) {
        if (null == str) {
            return null == testStr;
        }
        return str.toString().toLowerCase().contains(testStr.toString().toLowerCase());
    }

    public static String removeAll(String str, CharSequence strToRemove) {
        return str.replace(strToRemove, EMPTY);
    }

    public static String removePrefix(String str, String prefix) {
        if (isEmpty(str) || isEmpty(prefix)) {
            return str;
        }

        if (str.startsWith(prefix)) {
            return str.substring(prefix.length());
        }

        return str;
    }

    public static String removeSuffix(String str, String suffix) {
        if (isEmpty(str) || isEmpty(suffix)) {
            return str;
        }

        if (str.endsWith(suffix)) {
            return str.substring(0, str.length() - suffix.length());
        }

        return str;
    }

    public static String addPrefixIfMissing(String string, String prefix) {
        if(string.startsWith(prefix)) {
            return string;
        }

        return prefix + string;
    }

    public static String addSuffixIfMissing(String string, String suffix) {
        if(string.endsWith(suffix)){
            return string;
        }

        return string + suffix;
    }

    public static String upperFirst(CharSequence str) {
        if (null == str) {
            return null;
        }
        if (str.length() > 0) {
            char firstChar = str.charAt(0);
            if (Character.isLowerCase(firstChar)) {
                return Character.toUpperCase(firstChar) + substring(str, 1, str.length());
            }
        }
        return str.toString();
    }

    public static String lowerFirst(CharSequence str) {
        if (null == str) {
            return null;
        }
        if (str.length() > 0) {
            char firstChar = str.charAt(0);
            if (Character.isUpperCase(firstChar)) {
                return Character.toLowerCase(firstChar) + substring(str, 1, str.length());
            }
        }
        return str.toString();
    }

    public static String removeBlank(CharSequence str) {
        if (str == null) {
            return null;
        }

        int len = str.length();
        final StringBuilder sb = new StringBuilder(len);
        char c;
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            if (!Numbers.isBlankChar(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String substring(CharSequence string, int fromIndex, int toIndex) {
        int len = string.length();

        if (fromIndex < 0) {
            fromIndex = len + fromIndex;
            if (fromIndex < 0) {
                fromIndex = 0;
            }
        } else if (fromIndex > len) {
            fromIndex = len;
        }

        if (toIndex < 0) {
            toIndex = len + toIndex;
            if (toIndex < 0) {
                toIndex = len;
            }
        } else if (toIndex > len) {
            toIndex = len;
        }

        if (toIndex < fromIndex) {
            int tmp = fromIndex;
            fromIndex = toIndex;
            toIndex = tmp;
        }

        if (fromIndex == toIndex) {
            return EMPTY;
        }

        return string.toString().substring(fromIndex, toIndex);
    }

    public static boolean isSurroundWith(CharSequence str, CharSequence prefix, CharSequence suffix) {
        if (Strings.isBlank(str)) {
            return false;
        }
        if (str.length() < (prefix.length() + suffix.length())) {
            return false;
        }

        final String str2 = str.toString();
        return str2.startsWith(prefix.toString()) && str2.endsWith(suffix.toString());
    }

    public static boolean isSurroundWith(CharSequence str, char prefix, char suffix) {
        if (Strings.isBlank(str)) {
            return false;
        }
        if (str.length() < 2) {
            return false;
        }

        return str.charAt(0) == prefix && str.charAt(str.length() - 1) == suffix;
    }

    public static String repeat(char c, int count) {
        if (count <= 0) {
            return EMPTY;
        }

        char[] result = new char[count];
        for (int i = 0; i < count; i++) {
            result[i] = c;
        }
        return new String(result);
    }

    public static String repeat(CharSequence str, int count) {
        if (null == str) {
            return null;
        }
        if (count <= 0) {
            return EMPTY;
        }
        if (count == 1 || str.length() == 0) {
            return str.toString();
        }

        // check
        final int len = str.length();
        final long longSize = (long) len * (long) count;
        final int size = (int) longSize;
        if (size != longSize) {
            throw new ArrayIndexOutOfBoundsException("Required String length is too large: " + longSize);
        }

        final char[] array = new char[size];
        str.toString().getChars(0, len, array, 0);
        int n;
        for (n = len; n < size - n; n <<= 1) {// n <<= 1 means n *2
            System.arraycopy(array, 0, array, n, n);
        }
        System.arraycopy(array, 0, array, n, size - n);
        return new String(array);
    }

    public static boolean equals(CharSequence str1, CharSequence str2) {
        if (str1 == null) {
            return str2 == null;
        }

        return str1.equals(str2);
    }

    public static boolean equalsIgnoreCase(CharSequence str1, CharSequence str2) {
        if (str1 == null) {
            return str2 == null;
        }

        return str1.toString().equalsIgnoreCase(str2.toString());
    }

    public static String join(CharSequence conjunction, Object... objs) {
        return java.util.Arrays.stream(objs).map(Object::toString).collect(Collectors.joining(conjunction));
    }

    public static String toUnderlineCase(CharSequence camelCaseStr) {
        if (camelCaseStr == null) {
            return null;
        }

        final int length = camelCaseStr.length();
        StringBuilder sb = new StringBuilder();
        char c;
        boolean isPreUpperCase = false;
        for (int i = 0; i < length; i++) {
            c = camelCaseStr.charAt(i);
            boolean isNextUpperCase = true;
            if (i < (length - 1)) {
                isNextUpperCase = Character.isUpperCase(camelCaseStr.charAt(i + 1));
            }
            if (Character.isUpperCase(c)) {
                if (!isPreUpperCase || !isNextUpperCase) {
                    if (i > 0) sb.append(UNDERLINE);
                }
                isPreUpperCase = true;
            } else {
                isPreUpperCase = false;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    public static String toCamelCase(CharSequence name) {
        if (null == name) {
            return null;
        }

        String name2 = name.toString();
        if (name2.contains(UNDERLINE)) {
            name2 = name2.toLowerCase();

            StringBuilder sb = new StringBuilder(name2.length());
            boolean upperCase = false;
            for (int i = 0; i < name2.length(); i++) {
                char c = name2.charAt(i);

                if (c == '_') {
                    upperCase = true;
                } else if (upperCase) {
                    sb.append(Character.toUpperCase(c));
                    upperCase = false;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } else
            return name2;
    }

    public static StringBuilder builder() {
        return new StringBuilder();
    }

    public static StringBuilder builder(int capacity) {
        return new StringBuilder(capacity);
    }

    public static StringBuilder builder(CharSequence... strs) {
        final StringBuilder sb = new StringBuilder();
        for (CharSequence str : strs) {
            sb.append(str);
        }
        return sb;
    }

    public static int count(CharSequence content, CharSequence strForSearch) {
        if (containsEmpty(content, strForSearch) || strForSearch.length() > content.length()) {
            return 0;
        }

        int count = 0;
        int idx = 0;
        final String content2 = content.toString();
        final String strForSearch2 = strForSearch.toString();
        while ((idx = content2.indexOf(strForSearch2, idx)) > -1) {
            count++;
            idx += strForSearch.length();
        }
        return count;
    }

    public static int count(CharSequence content, char charForSearch) {
        int count = 0;
        if (isEmpty(content)) {
            return 0;
        }
        int contentLength = content.length();
        for (int i = 0; i < contentLength; i++) {
            if (charForSearch == content.charAt(i)) {
                count++;
            }
        }
        return count;
    }

    public static String[] cut(CharSequence str, int partLength) {
        if (null == str) {
            return null;
        }
        int len = str.length();
        if (len < partLength) {
            return new String[]{str.toString()};
        }
        int part = Numbers.count(len, partLength);
        final String[] array = new String[part];

        final String str2 = str.toString();
        for (int i = 0; i < part; i++) {
            array[i] = str2.substring(i * partLength, (i == part - 1) ? len : (partLength + i * partLength));
        }
        return array;
    }

    public static int indexOf(final CharSequence str, char searchChar) {
        return indexOf(str, searchChar, 0);
    }

    public static int indexOf(final CharSequence str, char searchChar, int start) {
        if (str instanceof String) {
            return ((String) str).indexOf(searchChar, start);
        } else {
            return indexOf(str, searchChar, start, -1);
        }
    }

    public static int indexOf(final CharSequence str, char searchChar, int start, int end) {
        final int len = str.length();
        if (start < 0 || start > len) {
            start = 0;
        }
        if (end > len || end < 0) {
            end = len;
        }
        for (int i = start; i < end; i++) {
            if (str.charAt(i) == searchChar) {
                return i;
            }
        }
        return -1;
    }

    public static String reverse(String str) {
        return new String(Arrays.reverse(str.toCharArray()));
    }

    public static String[] split(String str, String delimiter) {
        return str.split(delimiter);
    }

    public static byte[] bytes(CharSequence str) {
        return bytes(str, StandardCharsets.UTF_8);
    }

    public static byte[] bytes(CharSequence str, String charset) {
        return bytes(str, isBlank(charset) ? Charset.defaultCharset() : Charset.forName(charset));
    }

    public static byte[] bytes(CharSequence str, Charset charset) {
        if (str == null) {
            return null;
        }

        if (null == charset) {
            return str.toString().getBytes();
        }
        return str.toString().getBytes(charset);
    }

    public static String of(Object obj) {
        return of(obj, StandardCharsets.UTF_8);
    }

    public static String of(Object obj, String charsetName) {
        return of(obj, Charset.forName(charsetName));
    }

    public static String of(Object obj, Charset charset) {
        if (null == obj) {
            return null;
        }

        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof byte[]) {
            return of((byte[]) obj, charset);
        } else if (obj instanceof Byte[]) {
            return of((Byte[]) obj, charset);
        } else if (obj instanceof ByteBuffer) {
            return of((ByteBuffer) obj, charset);
        } else if (Arrays.isArray(obj)) {
            return Arrays.toString(obj);
        }

        return obj.toString();
    }

    public static String of(byte[] bytes, String charset) {
        return of(bytes, isBlank(charset) ? Charset.defaultCharset() : Charset.forName(charset));
    }

    public static String of(byte[] data, Charset charset) {
        if (data == null) {
            return null;
        }

        if (null == charset) {
            return new String(data);
        }
        return new String(data, charset);
    }

    public static String of(Byte[] bytes, String charset) {
        return of(bytes, isBlank(charset) ? Charset.defaultCharset() : Charset.forName(charset));
    }

    public static String of(Byte[] data, Charset charset) {
        if (data == null) {
            return null;
        }

        byte[] bytes = new byte[data.length];
        Byte dataByte;
        for (int i = 0; i < data.length; i++) {
            dataByte = data[i];
            bytes[i] = (null == dataByte) ? -1 : dataByte;
        }

        return of(bytes, charset);
    }

    public static String of(ByteBuffer data, String charset) {
        if (data == null) {
            return null;
        }

        return of(data, Charset.forName(charset));
    }

    public static String of(ByteBuffer data, Charset charset) {
        if (null == charset) {
            charset = Charset.defaultCharset();
        }
        return charset.decode(data).toString();
    }

    public static ByteBuffer byteBuffer(CharSequence str, String charset) {
        return ByteBuffer.wrap(bytes(str, charset));
    }
}