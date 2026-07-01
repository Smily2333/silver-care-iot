package com.silvercare.iot.geo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CoordinateTransform {

    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;

    private CoordinateTransform() {
    }

    public static BigDecimal wgs84ToGcj02Latitude(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return latitude;
        }
        double[] converted = wgs84ToGcj02(latitude.doubleValue(), longitude.doubleValue());
        return decimal(converted[0], latitude.scale());
    }

    public static BigDecimal wgs84ToGcj02Longitude(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return longitude;
        }
        double[] converted = wgs84ToGcj02(latitude.doubleValue(), longitude.doubleValue());
        return decimal(converted[1], longitude.scale());
    }

    private static double[] wgs84ToGcj02(double latitude, double longitude) {
        if (outOfChina(latitude, longitude)) {
            return new double[] { latitude, longitude };
        }
        double dLat = transformLat(longitude - 105.0, latitude - 35.0);
        double dLng = transformLng(longitude - 105.0, latitude - 35.0);
        double radLat = latitude / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * Math.PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * Math.PI);
        return new double[] { latitude + dLat, longitude + dLng };
    }

    private static boolean outOfChina(double latitude, double longitude) {
        return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271;
    }

    private static BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(Math.max(scale, 7), RoundingMode.HALF_UP);
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y
                + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x
                + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }
}
