package com.infilos.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("unused")
public final class Numbers {
    private Numbers() {
    }

    private static final int DIV_SCALE = 10;

    public static double add(double v1, double v2) {
        return add(Double.toString(v1), Double.toString(v2)).doubleValue();
    }

    public static double add(Double v1, Double v2) {
        return add(v1, (Number) v2).doubleValue();
    }

    public static BigDecimal add(Number v1, Number v2) {
        return add(v1.toString(), v2.toString());
    }

    public static BigDecimal add(String v1, String v2) {
        return add(new BigDecimal(v1), new BigDecimal(v2));
    }

    public static BigDecimal add(BigDecimal v1, BigDecimal v2) {
        Require.checkNotNull(v1); Require.checkNotNull(v2); return v1.add(v2);
    }

    public static double sub(double v1, double v2) {
        return sub(Double.toString(v1), Double.toString(v2)).doubleValue();
    }

    public static double sub(Double v1, Double v2) {
        return sub(v1, (Number) v2).doubleValue();
    }

    public static BigDecimal sub(Number v1, Number v2) {
        return sub(v1.toString(), v2.toString());
    }

    public static BigDecimal sub(String v1, String v2) {
        return sub(new BigDecimal(v1), new BigDecimal(v2));
    }

    public static BigDecimal sub(BigDecimal v1, BigDecimal v2) {
        Require.checkNotNull(v1); Require.checkNotNull(v2); return v1.subtract(v2);
    }

    public static double mul(double v1, double v2) {
        return mul(Double.toString(v1), Double.toString(v2)).doubleValue();
    }

    public static double mul(Double v1, Double v2) {
        return mul(v1, (Number) v2).doubleValue();
    }

    public static BigDecimal mul(Number v1, Number v2) {
        return mul(v1.toString(), v2.toString());
    }

    public static BigDecimal mul(String v1, String v2) {
        return mul(new BigDecimal(v1), new BigDecimal(v2));
    }

    public static BigDecimal mul(BigDecimal v1, BigDecimal v2) {
        Require.checkNotNull(v1); Require.checkNotNull(v2); return v1.multiply(v2);
    }

    public static double div(double v1, double v2) {
        return div(v1, v2, DIV_SCALE);
    }

    public static double div(Double v1, Double v2) {
        return div(v1, v2, DIV_SCALE);
    }

    public static BigDecimal div(Number v1, Number v2) {
        return div(v1, v2, DIV_SCALE);
    }

    public static BigDecimal div(String v1, String v2) {
        return div(v1, v2, DIV_SCALE);
    }

    public static double div(double v1, double v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    public static double div(Double v1, Double v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal div(Number v1, Number v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal div(String v1, String v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    public static double div(double v1, double v2, int scale, RoundingMode roundingMode) {
        return div(Double.toString(v1), Double.toString(v2), scale, roundingMode).doubleValue();
    }

    public static double div(Double v1, Double v2, int scale, RoundingMode roundingMode) {
        return div(v1, (Number) v2, scale, roundingMode).doubleValue();
    }

    public static BigDecimal div(Number v1, Number v2, int scale, RoundingMode roundingMode) {
        return div(v1.toString(), v2.toString(), scale, roundingMode);
    }

    public static BigDecimal div(String v1, String v2, int scale, RoundingMode roundingMode) {
        return div(new BigDecimal(v1), new BigDecimal(v2), scale, roundingMode);
    }

    public static BigDecimal div(BigDecimal v1, BigDecimal v2, int scale, RoundingMode roundingMode) {
        Require.checkNotNull(v1); Require.checkNotNull(v2); if (scale < 0) {
            scale = -scale;
        } return v1.divide(v2, scale, roundingMode);
    }

    public static double round(double v, int scale) {
        return round(v, scale, RoundingMode.HALF_UP);
    }

    public static double round(String numberStr, int scale) {
        return round(numberStr, scale, RoundingMode.HALF_UP);
    }

    public static double round(double v, int scale, RoundingMode roundingMode) {
        return round(Double.toString(v), scale, roundingMode);
    }

    public static double round(String numberStr, int scale, RoundingMode roundingMode) {
        final BigDecimal b = new BigDecimal(numberStr); return b.setScale(scale, roundingMode).doubleValue();
    }

    public static String roundStr(double number, int digit) {
        return String.format("%." + digit + 'f', number);
    }

    public static String decimalFormat(String pattern, double value) {
        return new DecimalFormat(pattern).format(value);
    }

    public static String decimalFormat(String pattern, long value) {
        return new DecimalFormat(pattern).format(value);
    }

    public static String decimalFormatMoney(Double value) {
        return decimalFormat(",###", value);
    }

    public static boolean isNumber(String str) {
        if (Strings.isBlank(str)) {
            return false;
        } char[] chars = str.toCharArray(); int sz = chars.length; boolean hasExp = false; boolean hasDecPoint = false; boolean allowSigns = false; boolean foundDigit = false;
        // deal with any possible sign up front
        int start = (chars[0] == '-') ? 1 : 0; if (sz > start + 1) {
            if (chars[start] == '0' && chars[start + 1] == 'x') {
                int i = start + 2; if (i == sz) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9') && (chars[i] < 'a' || chars[i] > 'f') && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                } return true;
            }
        } sz--; // don't want to loop to the last char, check it afterwords
        // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true; allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                } hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp) {
                    // two E's
                    return false;
                } if (!foundDigit) {
                    return false;
                } hasExp = true; allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                } allowSigns = false; foundDigit = false; // we need a digit after the E
            } else {
                return false;
            } i++;
        } if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            } if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            } if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                // single trailing decimal point after non-exponent is ok
                return foundDigit;
            } if (!allowSigns && (chars[i] == 'd' || chars[i] == 'D' || chars[i] == 'f' || chars[i] == 'F')) {
                return foundDigit;
            } if (chars[i] == 'l' || chars[i] == 'L') {
                // not allowing L with an exponent
                return foundDigit && !hasExp;
            }
            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }

    public static boolean isInteger(String s) {
        if (Strings.isNotBlank(s)) return s.matches("^\\d+$");
        else return false;
    }

    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return s.contains(".");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isPrimes(int n) {
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) {
                return false;
            }
        } return true;
    }

    public static int[] random(int begin, int end, int size) {
        if (begin > end) {
            int temp = begin; begin = end; end = temp;
        } if ((end - begin) < size) {
            throw new IllegalArgumentException("Size is larger than range between begin and end!");
        } int[] seed = new int[end - begin];

        for (int i = begin; i < end; i++) {
            seed[i - begin] = i;
        } int[] ranArr = new int[size]; Random ran = new Random(); for (int i = 0; i < size; i++) {
            int j = ran.nextInt(seed.length - i); ranArr[i] = seed[j]; seed[j] = seed[seed.length - 1 - i];
        }

        return ranArr;
    }

    public static Integer[] randomDistinct(int begin, int end, int size) {
        if (begin > end) {
            int temp = begin; begin = end; end = temp;
        } if ((end - begin) < size) {
            throw new IllegalArgumentException("Size is larger than range between begin and end!");
        }

        Random ran = new Random(); Set<Integer> set = new HashSet<>();

        while (set.size() < size) {
            set.add(begin + ran.nextInt(end - begin));
        }

        return set.toArray(new Integer[size]);
    }

    public static int[] range(int start, int stop) {
        return range(start, stop, 1);
    }

    public static int[] range(int start, int stop, int step) {
        if (start < stop) {
            step = Math.abs(step);
        } else if (start > stop) {
            step = -Math.abs(step);
        } else {// start == end
            return new int[]{start};
        }

        int size = Math.abs((stop - start) / step) + 1; int[] values = new int[size]; int index = 0; for (int i = start; (step > 0) ? i <= stop : i >= stop; i += step) {
            values[index] = i; index++;
        } return values;
    }

    public static int factorial(int n) {
        if (n == 1) {
            return 1;
        } return n * factorial(n - 1);
    }

    public static long sqrt(long x) {
        long y = 0; long b = (~Long.MAX_VALUE) >>> 1; while (b > 0) {
            if (x >= y + b) {
                x -= y + b; y >>= 1; y += b;
            } else {
                y >>= 1;
            } b >>= 2;
        } return y;
    }

    public static int divisor(int m, int n) {
        while (m % n != 0) {
            int temp = m % n; m = n; n = temp;
        } return n;
    }

    public static int multiple(int m, int n) {
        return m * n / divisor(m, n);
    }

    public static int compare(char x, char y) {
        return x - y;
    }

    public static int compare(double x, double y) {
        return Double.compare(x, y);
    }

    public static int compare(int x, int y) {
        return Integer.compare(x, y);
    }

    public static int compare(long x, long y) {
        return Long.compare(x, y);
    }

    public static int compare(short x, short y) {
        return Short.compare(x, y);
    }

    public static int compare(byte x, byte y) {
        return x - y;
    }

    public static boolean isGreater(BigDecimal bigNum1, BigDecimal bigNum2) {
        Require.checkNotNull(bigNum1); Require.checkNotNull(bigNum2); return bigNum1.compareTo(bigNum2) > 0;
    }

    public static boolean isGreaterOrEqual(BigDecimal bigNum1, BigDecimal bigNum2) {
        Require.checkNotNull(bigNum1); Require.checkNotNull(bigNum2); return bigNum1.compareTo(bigNum2) >= 0;
    }

    public static boolean isLess(BigDecimal bigNum1, BigDecimal bigNum2) {
        Require.checkNotNull(bigNum1); Require.checkNotNull(bigNum2); return bigNum1.compareTo(bigNum2) < 0;
    }

    public static boolean isLessOrEqual(BigDecimal bigNum1, BigDecimal bigNum2) {
        Require.checkNotNull(bigNum1); Require.checkNotNull(bigNum2); return bigNum1.compareTo(bigNum2) <= 0;
    }

    public static boolean equals(BigDecimal bigNum1, BigDecimal bigNum2) {
        Require.checkNotNull(bigNum1); Require.checkNotNull(bigNum2); return bigNum1.equals(bigNum2);
    }

    public static String toString(Number number, String defaultValue) {
        return (null == number) ? defaultValue : toString(number);
    }

    public static String toString(Number number) {
        if (null == number) {
            throw new NullPointerException("Number is null !");
        }

        if (!isValidIfNumber(number)) {
            throw new IllegalArgumentException("Number is non-finite!");
        }

        String string = number.toString(); if (string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            } if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        } return string;
    }

    public static boolean isValidIfNumber(Object obj) {
        if (obj instanceof Number) {
            if (obj instanceof Double) {
                return !((Double) obj).isInfinite() && !((Double) obj).isNaN();
            } else if (obj instanceof Float) {
                return !((Float) obj).isInfinite() && !((Float) obj).isNaN();
            }
        } return true;
    }

    public static boolean isBlankChar(char c) {
        return isBlankChar((int) c);
    }

    public static boolean isBlankChar(int c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    public static int count(int total, int part) {
        return (total % part == 0) ? (total / part) : (total / part + 1);
    }
}
