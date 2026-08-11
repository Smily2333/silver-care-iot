package com.silvercare.iot.api.dto;

import com.silvercare.iot.domain.DeviceActionStatus;
import com.silvercare.iot.domain.DeviceActionType;
import com.silvercare.iot.domain.entity.DeviceAction;

import java.time.Instant;

public record DeviceActionResponse(
        Long id,
        DeviceActionType type,
        DeviceActionStatus status,
        Instant requestedAt,
        Instant sentAt,
        Instant acknowledgedAt,
        Instant completedAt,
        Instant deadlineAt,
        String failureReason,
        Boolean ackMissing,
        String resultRecordType,
        Long resultRecordId
) {
    public static DeviceActionResponse from(DeviceAction action) {
        return new DeviceActionResponse(action.getId(), action.getActionType(), action.getStatus(),
                action.getRequestedAt(), action.getSentAt(), action.getAcknowledgedAt(),
                action.getCompletedAt(), action.getDeadlineAt(), action.getFailureReason(),
                action.getAckMissing(), action.getResultRecordType(), action.getResultRecordId());
    }
}
