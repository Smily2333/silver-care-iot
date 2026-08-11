package com.silvercare.iot.domain.entity;

import com.silvercare.iot.domain.DeviceActionStatus;
import com.silvercare.iot.domain.DeviceActionType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "device_actions", indexes = {
        @Index(name = "idx_device_action_device_time", columnList = "deviceId,requestedAt"),
        @Index(name = "idx_device_action_deadline", columnList = "status,deadlineAt")
})
public class DeviceAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long deviceId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeviceActionType actionType;
    @Column(nullable = false, length = 32)
    private String commandName;
    @Column(nullable = false, length = 255)
    private String commandContent;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeviceActionStatus status = DeviceActionStatus.PENDING;
    @Column(length = 64)
    private String requestedBy;
    private Instant requestedAt = Instant.now();
    private Instant sentAt;
    private Instant acknowledgedAt;
    private Instant completedAt;
    private Instant deadlineAt;
    @Column(length = 32)
    private String resultRecordType;
    private Long resultRecordId;
    @Column(length = 512)
    private String failureReason;
    private Boolean ackMissing = false;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getDeviceId() { return deviceId; }
    public DeviceActionType getActionType() { return actionType; }
    public String getCommandName() { return commandName; }
    public String getCommandContent() { return commandContent; }
    public DeviceActionStatus getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public String getResultRecordType() { return resultRecordType; }
    public Long getResultRecordId() { return resultRecordId; }
    public String getFailureReason() { return failureReason; }
    public Boolean getAckMissing() { return ackMissing; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setDeviceId(Long value) { deviceId = value; }
    public void setActionType(DeviceActionType value) { actionType = value; }
    public void setCommandName(String value) { commandName = value; }
    public void setCommandContent(String value) { commandContent = value; }
    public void setStatus(DeviceActionStatus value) { status = value; }
    public void setRequestedBy(String value) { requestedBy = value; }
    public void setSentAt(Instant value) { sentAt = value; }
    public void setAcknowledgedAt(Instant value) { acknowledgedAt = value; }
    public void setCompletedAt(Instant value) { completedAt = value; }
    public void setDeadlineAt(Instant value) { deadlineAt = value; }
    public void setResultRecordType(String value) { resultRecordType = value; }
    public void setResultRecordId(Long value) { resultRecordId = value; }
    public void setFailureReason(String value) { failureReason = value; }
    public void setAckMissing(Boolean value) { ackMissing = value; }
}
