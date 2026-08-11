package com.silvercare.iot.domain;

public enum DeviceActionStatus {
    PENDING,
    SENT,
    ACKNOWLEDGED,
    COMPLETED,
    PARTIAL_SUCCESS,
    SEND_FAILED,
    ACK_TIMEOUT,
    RESULT_TIMEOUT,
    CANCELLED;

    public boolean isTerminal() {
        return switch (this) {
            case COMPLETED, PARTIAL_SUCCESS, SEND_FAILED, ACK_TIMEOUT, RESULT_TIMEOUT, CANCELLED -> true;
            default -> false;
        };
    }
}
