package com.silvercare.iot.domain.entity;

import com.silvercare.iot.domain.HealthMeasurementStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "health_records", indexes = {
        @Index(name = "idx_health_device_time", columnList = "deviceId,measuredAt")
})
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    private Integer heartRate;
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private Integer heightCm;
    private Integer genderCode;
    private Integer age;
    private Integer weightKg;
    private Integer temperatureType;
    private BigDecimal bodyTemperature;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private HealthMeasurementStatus heartRateStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private HealthMeasurementStatus bloodPressureStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private HealthMeasurementStatus temperatureStatus;

    @Column(length = 255)
    private String invalidReason;

    @Column(nullable = false, length = 32)
    private String sourceCommand;

    private Long rawPacketId;

    @Column(nullable = false)
    private Instant measuredAt = Instant.now();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getId() {
        return id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public Integer getSystolicPressure() {
        return systolicPressure;
    }

    public Integer getDiastolicPressure() {
        return diastolicPressure;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public Integer getGenderCode() {
        return genderCode;
    }

    public Integer getAge() {
        return age;
    }

    public Integer getWeightKg() {
        return weightKg;
    }

    public Integer getTemperatureType() {
        return temperatureType;
    }

    public BigDecimal getBodyTemperature() {
        return bodyTemperature;
    }

    public HealthMeasurementStatus getHeartRateStatus() {
        return heartRateStatus;
    }

    public HealthMeasurementStatus getBloodPressureStatus() {
        return bloodPressureStatus;
    }

    public HealthMeasurementStatus getTemperatureStatus() {
        return temperatureStatus;
    }

    public String getInvalidReason() {
        return invalidReason;
    }

    public String getSourceCommand() {
        return sourceCommand;
    }

    public Long getRawPacketId() {
        return rawPacketId;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public void setSystolicPressure(Integer systolicPressure) {
        this.systolicPressure = systolicPressure;
    }

    public void setDiastolicPressure(Integer diastolicPressure) {
        this.diastolicPressure = diastolicPressure;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public void setGenderCode(Integer genderCode) {
        this.genderCode = genderCode;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setWeightKg(Integer weightKg) {
        this.weightKg = weightKg;
    }

    public void setTemperatureType(Integer temperatureType) {
        this.temperatureType = temperatureType;
    }

    public void setBodyTemperature(BigDecimal bodyTemperature) {
        this.bodyTemperature = bodyTemperature;
    }

    public void setHeartRateStatus(HealthMeasurementStatus heartRateStatus) {
        this.heartRateStatus = heartRateStatus;
    }

    public void setBloodPressureStatus(HealthMeasurementStatus bloodPressureStatus) {
        this.bloodPressureStatus = bloodPressureStatus;
    }

    public void setTemperatureStatus(HealthMeasurementStatus temperatureStatus) {
        this.temperatureStatus = temperatureStatus;
    }

    public void setInvalidReason(String invalidReason) {
        this.invalidReason = invalidReason;
    }

    public void setSourceCommand(String sourceCommand) {
        this.sourceCommand = sourceCommand;
    }

    public void setRawPacketId(Long rawPacketId) {
        this.rawPacketId = rawPacketId;
    }
}
