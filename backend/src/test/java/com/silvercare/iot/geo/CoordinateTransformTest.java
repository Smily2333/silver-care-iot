package com.silvercare.iot.geo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CoordinateTransformTest {

    @Test
    void wgs84ToGcj02_convertsMainlandChinaCoordinate() {
        BigDecimal lat = new BigDecimal("22.570720");
        BigDecimal lng = new BigDecimal("113.8620167");

        BigDecimal mapLat = CoordinateTransform.wgs84ToGcj02Latitude(lat, lng);
        BigDecimal mapLng = CoordinateTransform.wgs84ToGcj02Longitude(lat, lng);

        assertThat(mapLat).isNotEqualByComparingTo(lat);
        assertThat(mapLng).isNotEqualByComparingTo(lng);
        assertThat(mapLat).isBetween(new BigDecimal("22.56"), new BigDecimal("22.58"));
        assertThat(mapLng).isBetween(new BigDecimal("113.86"), new BigDecimal("113.88"));
    }

    @Test
    void wgs84ToGcj02_keepsCoordinatesOutsideChina() {
        BigDecimal lat = new BigDecimal("37.7749000");
        BigDecimal lng = new BigDecimal("-122.4194000");

        assertThat(CoordinateTransform.wgs84ToGcj02Latitude(lat, lng)).isEqualByComparingTo(lat);
        assertThat(CoordinateTransform.wgs84ToGcj02Longitude(lat, lng)).isEqualByComparingTo(lng);
    }
}
