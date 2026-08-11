package com.silvercare.iot.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeviceActionTimeoutScheduler {
    private final DeviceActionService service;

    public DeviceActionTimeoutScheduler(DeviceActionService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${silver-care.device-actions.timeout-scan-millis:10000}")
    public void expireOverdue() { service.expireOverdue(); }
}
