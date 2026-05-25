package com.silvercare.iot.domain.entity;

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

    public void setSourceCommand(String sourceCommand) {
        this.sourceCommand = sourceCommand;
    }

    public void setRawPacketId(Long rawPacketId) {
        this.rawPacketId = rawPacketId;
    }
}
