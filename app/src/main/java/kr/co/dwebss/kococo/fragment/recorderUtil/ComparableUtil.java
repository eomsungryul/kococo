package kr.co.dwebss.kococo.fragment.recorderUtil;

import java.math.BigDecimal;
import java.util.Date;

public class ComparableUtil {
    public ComparableUtil() {
    }

    public static boolean isDefined(double arg) {
        return arg != 1.7976931348623157E308D && arg != -1.7976931348623157E308D && !Double.isInfinite(arg) && !Double.isNaN(arg);
    }

    public static boolean isDefined(float arg) {
        return arg != 3.4028235E38F && arg != -3.4028235E38F && !Float.isInfinite(arg) && !Float.isNaN(arg);
    }

    public static boolean isDefined(int arg) {
        return arg != 2147483647 && arg != -2147483648;
    }

    public static boolean isDefined(short arg) {
        return arg != 32767 && arg != -32768;
    }

    public static boolean isDefined(byte arg) {
        return arg != 127 && arg != -128;
    }

    public static boolean isDefined(long arg) {
        return arg != 9223372036854775807L && arg != -9223372036854775808L;
    }

    public static boolean isDefined(Date arg) {
        long var1 = arg.getTime();
        return var1 != 0L && var1 != 315555177599999L;
    }

    public static boolean canConvertToFloat(double value) {
        return isDefined(value) && value >= -3.4028234663852886E38D && value <= 3.4028234663852886E38D;
    }

    public static boolean canConvertToLong(double value) {
        return isDefined(value) && value > -9.223372036854776E18D && value < 9.223372036854776E18D;
    }

    public static boolean canConvertToInteger(double value) {
        return isDefined(value) && value >= -2.147483648E9D && value <= 2.147483647E9D;
    }

    public static boolean canConvertToShort(double value) {
        return isDefined(value) && value >= -32768.0D && value <= 32767.0D;
    }

    public static boolean canConvertToByte(double value) {
        return isDefined(value) && value >= -128.0D && value <= 127.0D;
    }

    public static boolean canConvertToDate(double value) {
        return canConvertToLong(value) && value >= 0.0D;
    }

    public static boolean canConvertToBigDecimal(double value) {
        return isDefined(value);
    }

    private static boolean a(Comparable var0) {
        if (var0 instanceof Double) {
            return isDefined((Double)var0);
        } else if (var0 instanceof Float) {
            return isDefined((Float)var0);
        } else if (var0 instanceof Long) {
            return isDefined((Long)var0);
        } else if (var0 instanceof Integer) {
            return isDefined((Integer)var0);
        } else if (var0 instanceof Short) {
            return isDefined((Short)var0);
        } else if (var0 instanceof Byte) {
            return isDefined((Byte)var0);
        } else if (var0 instanceof Date) {
            return isDefined((Date)var0);
        } else if (var0 instanceof BigDecimal) {
            return true;
        } else {
            throw new UnsupportedOperationException(String.format("The %s is not a valid Comparable Type", var0.toString()));
        }
    }

    public static double toDouble(Comparable comparable) {
        if (comparable instanceof Date) {
            return (double)((Date)comparable).getTime();
        } else if (comparable instanceof Number) {
            return ((Number)comparable).doubleValue();
        } else {
            throw new IllegalArgumentException(String.format("The type %s could not be converted to double ", comparable.getClass().getName()));
        }
    }

    public static Date toDate(Comparable comparable) {
        if (comparable instanceof Date) {
            return (Date)comparable;
        } else if (comparable instanceof Number) {
            long var1 = NumberUtil.constrain(((Number)comparable).longValue(), 0L, 315555177599999L);
            return new Date(var1);
        } else {
            return new Date();
        }
    }

    public static boolean isDefined(Comparable comparable) {
        return comparable != null && a(comparable);
    }

    public static <T extends Comparable<T>> double fromDouble(double rawDataValue, Class<T> type) {
    	return rawDataValue;
    	/*
        if (type == null) {
            return null;
        } else if (type == Double.class) {
            return rawDataValue;
        } else if (type == Float.class) {
            return canConvertToFloat(rawDataValue) ? (float)rawDataValue : null;
        } else if (type == Long.class) {
            return canConvertToLong(rawDataValue) ? (long)rawDataValue : null;
        } else if (type == Integer.class) {
            return canConvertToInteger(rawDataValue) ? (int)rawDataValue : null;
        } else if (type == Short.class) {
            return canConvertToShort(rawDataValue) ? (short)((int)rawDataValue) : null;
        } else if (type == Byte.class) {
            return canConvertToByte(rawDataValue) ? (byte)((int)rawDataValue) : null;
        } else if (type == BigDecimal.class) {
            return canConvertToBigDecimal(rawDataValue) ? BigDecimal.valueOf(rawDataValue) : null;
        } else if (type == Date.class) {
            return canConvertToDate(rawDataValue) ? new Date((long)rawDataValue) : null;
        } else {
            return null;
        }
        */
    }

    public static <T extends Comparable<T>> T min(T value1, T value2) {
        return value1.compareTo(value2) < 0 ? value1 : value2;
    }

    public static <T extends Comparable<T>> T max(T value1, T value2) {
        return value1.compareTo(value2) > 0 ? value1 : value2;
    }

    public static <T extends Comparable<T>> int compare(T a, T b) {
        return a.compareTo(b);
    }
}
