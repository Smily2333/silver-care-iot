package com.silvercare.iot.domain.entity;

import com.silvercare.iot.domain.DeviceStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_devices_device_no", columnList = "deviceNo", unique = true)
})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String deviceNo;

    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    private Integer batteryLevel;
    private Integer stepCount;
    private Instant lastOnlineAt;
    private Instant lastHeartbeatAt;

    @Column(length = 64)
    private String ownerName;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getDeviceNo() {
        return deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        this.deviceNo = deviceNo;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Integer getStepCount() {
        return stepCount;
    }

    public void setStepCount(Integer stepCount) {
        this.stepCount = stepCount;
    }

    public Instant getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(Instant lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}
