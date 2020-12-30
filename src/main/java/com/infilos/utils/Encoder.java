package com.infilos.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public final class Encoder {
    private Encoder() {
    }

    public static String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                String haxHex = Integer.toHexString(aByte & 0xFF);
                if (haxHex.length() < 2) {
                    sb.append("0");
                }
                sb.append(haxHex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Encode md5 failed, " + e.getMessage(), e);
        }
    }

    public static String asBase64(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String ofBase64(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    private static final char[] Digits = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    /**
     * 2 <= seed <= 62
     */
    public static String decimalToNumeric(long dec, int seed) {
        if (dec < 0) {
            dec = ((long) 2 * 0x7fffffff) + dec + 2;
        }
        char[] buf = new char[32];
        int charPos = 32;
        while ((dec / seed) > 0) {
            buf[--charPos] = Digits[(int) (dec % seed)];
            dec /= seed;
        }
        buf[--charPos] = Digits[(int) (dec % seed)];
        return new String(buf, charPos, (32 - charPos));
    }

    /**
     * 2 <= seed <= 62
     */
    public static long numericToDecimal(String num, int seed) {
        char[] charBuf = num.toCharArray();
        if (seed == 10) {
            return Long.parseLong(num);
        }

        long result = 0, base = 1;

        for (int i = charBuf.length - 1; i >= 0; i--) {
            int index = 0;
            for (int j = 0, length = Digits.length; j < length; j++) {
                if (Digits[j] == charBuf[i]) {
                    index = j;
                }
            }
            result += index * base;
            base *= seed;
        }
        return result;
    }
}