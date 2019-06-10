package kr.co.dwebss.kococo.fragment.recorderUtil;

public class DoubleUtil {
    private static final double[] a = new double[]{1.0D, 10.0D, 100.0D, 1000.0D, 10000.0D, 100000.0D, 1000000.0D, 1.0E7D, 1.0E8D, 1.0E9D, 1.0E10D, 1.0E11D, 1.0E12D, 1.0E13D, 1.0E14D, 1.0E15D};
    public static final double EPSILON = 1.0E-15D;

    public DoubleUtil() {
    }

    public static double roundUp(double value, double nearest) {
        return Math.ceil(value / nearest) * nearest;
    }

    public static double roundDown(double value, double nearest) {
        return Math.floor(value / nearest) * nearest;
    }

    public static boolean isDivisibleBy(double value, double divisor) {
        value = round(value, 15);
        if (Math.abs(divisor) < 1.0E-15D) {
            return false;
        } else {
            double var4 = Math.abs(value / divisor);
            double var6 = 1.0E-15D * var4;
            return Math.abs(var4 - (double)Math.round(var4)) <= var6;
        }
    }

    public static boolean isRealNumber(double number) {
        return ComparableUtil.isDefined(number);
    }

    public static boolean isZero(double number) {
        return Math.abs(number) <= 1.0E-15D;
    }

    public static double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }

    public static boolean isPowerOf(double value, double power, double logBase) {
        return Math.abs(roundUpPower(value, power, logBase) - value) <= 1.0E-15D;
    }

    public static double roundUpPower(double value, double power, double logBase) {
        boolean var6 = Math.signum(value) == -1.0D;
        double var7 = log(Math.abs(value), logBase) / log(Math.abs(power), logBase);
        var7 = round(var7, 5);
        double var9 = Math.ceil(var7);
        if (Math.abs(var9 - var7) < 1.0E-15D) {
            return value;
        } else {
            var9 = var6 ? var9 - 1.0D : var9;
            double var11 = Math.pow(power, var9);
            return var6 ? -var11 : var11;
        }
    }

    public static double roundDownPower(double value, double power, double logBase) {
        boolean var6 = Math.signum(value) == -1.0D;
        double var7 = log(Math.abs(value), logBase) / log(Math.abs(power), logBase);
        var7 = round(var7, 5);
        double var9 = Math.floor(var7);
        if (Math.abs(var9 - var7) < 1.0E-15D) {
            return value;
        } else {
            var9 = var6 ? var9 - 1.0D : var9;
            double var11 = Math.pow(power, var9);
            return var6 ? -var11 : var11;
        }
    }

    public static double roundOff(double value, int decimals, boolean toNearest) {
        double var4 = a[decimals];
        value *= var4;
        if (toNearest) {
            value = (double)Math.round(value);
        } else {
            value = Math.rint(value);
        }

        value /= var4;
        return value;
    }

    public static double round(double value, int decimals) {
        return roundOff(value, decimals, false);
    }

    public static double roundOff(double value) {
        return (double)Math.round(value);
    }
}
