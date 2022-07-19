package com.infilos.utils;

import java.nio.ByteOrder;

@SuppressWarnings("unused")
public final class Bytes {
    private Bytes() {
    }

    public static byte castOfInt(int intValue) {
        return (byte) intValue;
    }

    public static int toUnsignedInt(byte byteValue) {
        // '& 0xFF' to unsigned
        return byteValue & 0xFF;
    }

    public static short toShort(byte[] bytes) {
        return toShort(bytes, ByteOrder.LITTLE_ENDIAN);
    }

    public static short toShort(byte[] bytes, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN == byteOrder) {
            return (short) (bytes[0] & 0xff | (bytes[1] & 0xff) << Byte.SIZE);
        } else {
            return (short) (bytes[1] & 0xff | (bytes[0] & 0xff) << Byte.SIZE);
        }
    }

    public static byte[] ofShort(short shortValue) {
        return ofShort(shortValue, ByteOrder.LITTLE_ENDIAN);
    }

    public static byte[] ofShort(short shortValue, ByteOrder byteOrder) {
        byte[] b = new byte[Short.BYTES];
        if (ByteOrder.LITTLE_ENDIAN == byteOrder) {
            b[0] = (byte) (shortValue & 0xff);
            b[1] = (byte) ((shortValue >> Byte.SIZE) & 0xff);
        } else {
            b[1] = (byte) (shortValue & 0xff);
            b[0] = (byte) ((shortValue >> Byte.SIZE) & 0xff);
        }
        return b;
    }

    public static int toInt(byte[] bytes) {
        return toInt(bytes, ByteOrder.LITTLE_ENDIAN);
    }

    public static int toInt(byte[] bytes, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN == byteOrder) {
            return bytes[0] & 0xFF
                | (bytes[1] & 0xFF) << 8
                | (bytes[2] & 0xFF) << 16
                | (bytes[3] & 0xFF) << 24;
        } else {
            return bytes[3] & 0xFF
                | (bytes[2] & 0xFF) << 8
                | (bytes[1] & 0xFF) << 16
                | (bytes[0] & 0xFF) << 24;
        }
    }

    public static byte[] ofInt(int intValue) {
        return ofInt(intValue, ByteOrder.LITTLE_ENDIAN);
    }

    public static byte[] ofInt(int intValue, ByteOrder byteOrder) {
        if (ByteOrder.LITTLE_ENDIAN == byteOrder) {
            return new byte[]{
                (byte) (intValue & 0xFF),
                (byte) ((intValue >> 8) & 0xFF),
                (byte) ((intValue >> 16) & 0xFF),
                (byte) ((intValue >> 24) & 0xFF)
            };

        } else {
            return new byte[]{
                (byte) ((intValue >> 24) & 0xFF),
                (byte) ((intValue >> 16) & 0xFF),
                (byte) ((intValue >> 8) & 0xFF),
                (byte) (intValue & 0xFF)
            };
        }
    }

    public static byte[] ofLong(long longValue) {
        return ofLong(longValue, ByteOrder.LITTLE_ENDIAN);
    }

    public static byte[] ofLong(long longValue, ByteOrder byteOrder) {
        byte[] result = new byte[Long.BYTES];
        if (ByteOrder.LITTLE_ENDIAN == byteOrder) {
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) (longValue & 0xFF);
                longValue >>= Byte.SIZE;
            }
        } else {
            for (int i = (result.length - 1); i >= 0; i--) {
                result[i] = (byte) (longValue & 0xFF);
                longValue >>= Byte.SIZE;
            }
        }
        return result;
    }

    public static long toLong(byte[] bytes) {
        return toLong(bytes, ByteOrder.LITTLE_ENDIAN);
    }

    public static long toLong(byte[] bytes, ByteOrder byteOrder) {
        long values = 0;
        if (ByteOrder.LITTLE_ENDIAN == byteOrder) {
            for (int i = (Long.BYTES - 1); i >= 0; i--) {
                values <<= Byte.SIZE;
                values |= (bytes[i] & 0xff);
            }
        } else {
            for (int i = 0; i < Long.BYTES; i++) {
                values <<= Byte.SIZE;
                values |= (bytes[i] & 0xff);
            }
        }

        return values;
    }

    public static byte[] ofDouble(double doubleValue) {
        return ofDouble(doubleValue, ByteOrder.LITTLE_ENDIAN);
    }

    public static byte[] ofDouble(double doubleValue, ByteOrder byteOrder) {
        return ofLong(Double.doubleToLongBits(doubleValue), byteOrder);
    }

    public static double toDouble(byte[] bytes) {
        return toDouble(bytes, ByteOrder.LITTLE_ENDIAN);
    }

    public static double toDouble(byte[] bytes, ByteOrder byteOrder) {
        return Double.longBitsToDouble(toLong(bytes, byteOrder));
    }

    public static byte[] ofNumber(Number number) {
        return ofNumber(number, ByteOrder.LITTLE_ENDIAN);
    }

    public static byte[] ofNumber(Number number, ByteOrder byteOrder) {
        if (number instanceof Double) {
            return ofDouble((Double) number, byteOrder);
        } else if (number instanceof Long) {
            return ofLong((Long) number, byteOrder);
        } else if (number instanceof Integer) {
            return ofInt((Integer) number, byteOrder);
        } else if (number instanceof Short) {
            return ofShort((Short) number, byteOrder);
        } else {
            return ofDouble(number.doubleValue(), byteOrder);
        }
    }
}
