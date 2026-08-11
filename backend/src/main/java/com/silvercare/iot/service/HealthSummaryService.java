package com.silvercare.iot.service;

import com.silvercare.iot.api.dto.AdminHealthSummaryResponse;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.repository.HealthRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class HealthSummaryService {

    private final HealthRecordRepository repository;
    private final Duration freshness;

    public HealthSummaryService(HealthRecordRepository repository,
                                @Value("${silver-care.health.freshness-minutes:180}") long freshnessMinutes) {
        this.repository = repository;
        this.freshness = Duration.ofMinutes(Math.max(1, freshnessMinutes));
    }

    public AdminHealthSummaryResponse get(Long deviceId) {
        HealthRecord heart = repository.findFirstByDeviceIdAndHeartRateIsNotNullOrderByMeasuredAtDesc(deviceId).orElse(null);
        HealthRecord pressure = repository.findFirstByDeviceIdAndSystolicPressureIsNotNullAndDiastolicPressureIsNotNullOrderByMeasuredAtDesc(deviceId).orElse(null);
        HealthRecord temperature = repository.findFirstByDeviceIdAndBodyTemperatureIsNotNullOrderByMeasuredAtDesc(deviceId).orElse(null);
        HealthRecord oxygen = repository.findFirstByDeviceIdAndOxygenSaturationIsNotNullOrderByMeasuredAtDesc(deviceId).orElse(null);
        return new AdminHealthSummaryResponse(
                heart == null ? null : new AdminHealthSummaryResponse.Metric<>(
                        heart.getHeartRate(), heart.getMeasuredAt(), heart.getHeartRateStatus(), freshness(heart.getMeasuredAt())),
                pressure == null ? null : new AdminHealthSummaryResponse.BloodPressureMetric(
                        pressure.getSystolicPressure(), pressure.getDiastolicPressure(), pressure.getMeasuredAt(),
                        pressure.getBloodPressureStatus(), freshness(pressure.getMeasuredAt())),
                temperature == null ? null : new AdminHealthSummaryResponse.Metric<>(
                        temperature.getBodyTemperature(), temperature.getMeasuredAt(), temperature.getTemperatureStatus(),
                        freshness(temperature.getMeasuredAt())),
                oxygen == null ? null : new AdminHealthSummaryResponse.Metric<>(
                        oxygen.getOxygenSaturation(), oxygen.getMeasuredAt(), oxygen.getOxygenStatus(),
                        freshness(oxygen.getMeasuredAt()))
        );
    }

    private String freshness(Instant measuredAt) {
        if (measuredAt == null) return "UNKNOWN";
        return measuredAt.isBefore(Instant.now().minus(freshness)) ? "STALE" : "FRESH";
    }
}
