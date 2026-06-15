package com.silvercare.iot.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "location_records", indexes = {
        @Index(name = "idx_location_device_time", columnList = "deviceId,locatedAt")
})
public class LocationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(precision = 11, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 12, scale = 7)
    private BigDecimal longitude;
    private String latitudeHemisphere;
    private String longitudeHemisphere;
    private Boolean gpsValid;
    @Column(precision = 8, scale = 3)
    private BigDecimal speed;

    @Column(precision = 8, scale = 3)
    private BigDecimal direction;

    @Column(precision = 8, scale = 3)
    private BigDecimal altitude;
    private Integer satelliteCount;
    private Integer gsmSignal;
    private Integer batteryLevel;
    private Integer stepCount;
    private Integer rolloverCount;
    private String terminalStatus;
    private BigDecimal accuracy;

    @Column(length = 32)
    private String sourceCommand;

    private Long rawPacketId;

    @Column(nullable = false)
    private Instant locatedAt = Instant.now();

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

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getLatitudeHemisphere() {
        return latitudeHemisphere;
    }

    public String getLongitudeHemisphere() {
        return longitudeHemisphere;
    }

    public Boolean getGpsValid() {
        return gpsValid;
    }

    public BigDecimal getSpeed() {
        return speed;
    }

    public BigDecimal getDirection() {
        return direction;
    }

    public BigDecimal getAltitude() {
        return altitude;
    }

    public Integer getSatelliteCount() {
        return satelliteCount;
    }

    public Integer getGsmSignal() {
        return gsmSignal;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public Integer getStepCount() {
        return stepCount;
    }

    public Integer getRolloverCount() {
        return rolloverCount;
    }

    public String getTerminalStatus() {
        return terminalStatus;
    }

    public BigDecimal getAccuracy() {
        return accuracy;
    }

    public String getSourceCommand() {
        return sourceCommand;
    }

    public Long getRawPacketId() {
        return rawPacketId;
    }

    public Instant getLocatedAt() {
        return locatedAt;
    }

    public void setLocatedAt(Instant locatedAt) {
        this.locatedAt = locatedAt;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public void setLatitudeHemisphere(String latitudeHemisphere) {
        this.latitudeHemisphere = latitudeHemisphere;
    }

    public void setLongitudeHemisphere(String longitudeHemisphere) {
        this.longitudeHemisphere = longitudeHemisphere;
    }

    public void setGpsValid(Boolean gpsValid) {
        this.gpsValid = gpsValid;
    }

    public void setSpeed(BigDecimal speed) {
        this.speed = speed;
    }

    public void setDirection(BigDecimal direction) {
        this.direction = direction;
    }

    public void setAltitude(BigDecimal altitude) {
        this.altitude = altitude;
    }

    public void setSatelliteCount(Integer satelliteCount) {
        this.satelliteCount = satelliteCount;
    }

    public void setGsmSignal(Integer gsmSignal) {
        this.gsmSignal = gsmSignal;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public void setStepCount(Integer stepCount) {
        this.stepCount = stepCount;
    }

    public void setRolloverCount(Integer rolloverCount) {
        this.rolloverCount = rolloverCount;
    }

    public void setTerminalStatus(String terminalStatus) {
        this.terminalStatus = terminalStatus;
    }

    public void setAccuracy(BigDecimal accuracy) {
        this.accuracy = accuracy;
    }

    public void setSourceCommand(String sourceCommand) {
        this.sourceCommand = sourceCommand;
    }

    public void setRawPacketId(Long rawPacketId) {
        this.rawPacketId = rawPacketId;
    }
}
