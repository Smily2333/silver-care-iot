package com.silvercare.iot.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fall_alerts", indexes = {
        @Index(name = "idx_fall_alert_device_time", columnList = "deviceId,alertedAt")
})
public class FallAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(precision = 11, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 12, scale = 7)
    private BigDecimal longitude;

    private Boolean gpsValid;
    private Long locationRecordId;
    private Long rawPacketId;

    @Column(nullable = false)
    private Instant alertedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getDeviceId() { return deviceId; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public Boolean getGpsValid() { return gpsValid; }
    public Long getLocationRecordId() { return locationRecordId; }
    public Long getRawPacketId() { return rawPacketId; }
    public Instant getAlertedAt() { return alertedAt; }

    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setGpsValid(Boolean gpsValid) { this.gpsValid = gpsValid; }
    public void setLocationRecordId(Long locationRecordId) { this.locationRecordId = locationRecordId; }
    public void setRawPacketId(Long rawPacketId) { this.rawPacketId = rawPacketId; }
    public void setAlertedAt(Instant alertedAt) { this.alertedAt = alertedAt; }
}
