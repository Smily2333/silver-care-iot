package com.silvercare.iot.api.dto;

import com.silvercare.iot.domain.entity.LocationRecord;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminLocationRecordResponse(
        Long id,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean gpsValid,
        BigDecimal speed,
        BigDecimal direction,
        BigDecimal altitude,
        Integer satelliteCount,
        Integer gsmSignal,
        Integer batteryLevel,
        BigDecimal accuracy,
        Instant locatedAt,
        String approximateAddress,
        String addressStatus
) {
    public static AdminLocationRecordResponse from(LocationRecord record) {
        return new AdminLocationRecordResponse(
                record.getId(),
                record.getLatitude(),
                record.getLongitude(),
                record.getGpsValid(),
                record.getSpeed(),
                record.getDirection(),
                record.getAltitude(),
                record.getSatelliteCount(),
                record.getGsmSignal(),
                record.getBatteryLevel(),
                record.getAccuracy(),
                record.getLocatedAt(),
                record.getApproximateAddress(),
                record.getAddressStatus() == null ? null : record.getAddressStatus().name()
        );
    }
}
