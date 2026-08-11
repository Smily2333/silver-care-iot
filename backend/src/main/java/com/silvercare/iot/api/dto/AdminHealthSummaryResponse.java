package com.silvercare.iot.api.dto;

import com.silvercare.iot.domain.HealthMeasurementStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminHealthSummaryResponse(
        Metric<Integer> heartRate,
        BloodPressureMetric bloodPressure,
        Metric<BigDecimal> temperature
) {
    public record Metric<T>(T value, Instant measuredAt, HealthMeasurementStatus status, String freshness) {}

    public record BloodPressureMetric(Integer systolic, Integer diastolic, Instant measuredAt,
                                      HealthMeasurementStatus status, String freshness) {}
}
