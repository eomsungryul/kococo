package kr.co.dwebss.kococo.fragment.recorderUtil;

public class NumberUtil {
    public NumberUtil() {
    }

    public static int constrain(int value, int lowerBound, int upperBound) {
        return value < lowerBound ? lowerBound : (value > upperBound ? upperBound : value);
    }

    public static long constrain(long value, long lowerBound, long upperBound) {
        return value < lowerBound ? lowerBound : (value > upperBound ? upperBound : value);
    }

    public static float constrain(float value, float lowerBound, float upperBound) {
        return value < lowerBound ? lowerBound : (value > upperBound ? upperBound : value);
    }

    public static double constrain(double value, double lowerBound, double upperBound) {
        return value < lowerBound ? lowerBound : (value > upperBound ? upperBound : value);
    }

    public static boolean isIntegerType(Class<?> type) {
        return type == Byte.class || type == Short.class || type == Integer.class || type == Long.class;
    }
}
