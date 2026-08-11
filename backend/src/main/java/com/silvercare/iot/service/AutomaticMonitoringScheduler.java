package com.silvercare.iot.service;

import com.silvercare.iot.config.AutomaticMonitoringProperties;
import com.silvercare.iot.domain.DeviceActionStatus;
import com.silvercare.iot.domain.DeviceActionType;
import com.silvercare.iot.domain.HealthMeasurementStatus;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.DeviceAction;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.repository.DeviceActionRepository;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;

@Component
public class AutomaticMonitoringScheduler {
    private static final Logger log = LoggerFactory.getLogger(AutomaticMonitoringScheduler.class);
    private static final String REQUESTED_BY = "AUTO_MONITOR";
    private static final EnumSet<DeviceActionStatus> ACTIVE = EnumSet.of(
            DeviceActionStatus.PENDING, DeviceActionStatus.SENT, DeviceActionStatus.ACKNOWLEDGED);

    private final DeviceRepository deviceRepository;
    private final DeviceActionRepository actionRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final DeviceActionService actionService;
    private final AutomaticMonitoringProperties properties;

    public AutomaticMonitoringScheduler(DeviceRepository deviceRepository,
                                        DeviceActionRepository actionRepository,
                                        HealthRecordRepository healthRecordRepository,
                                        DeviceActionService actionService,
                                        AutomaticMonitoringProperties properties) {
        this.deviceRepository = deviceRepository;
        this.actionRepository = actionRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.actionService = actionService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${silver-care.automatic-monitoring.scan-millis:60000}")
    public void runCycle() {
        if (!properties.isEnabled()) return;
        Instant now = Instant.now();
        deviceRepository.findAll().forEach(device -> scheduleNext(device, now));
    }

    private void scheduleNext(Device device, Instant now) {
        if (device.getId() == null || isBatteryLow(device)) return;
        if (actionRepository.findFirstByDeviceIdAndStatusInOrderByRequestedAtDesc(device.getId(), ACTIVE).isPresent()) {
            return;
        }
        if (isDue(device.getId(), DeviceActionType.CONFIGURE_LOCATION_INTERVAL,
                Duration.ofHours(properties.getLocationReapplyHours()), now)
                && tryCreate(device, DeviceActionType.CONFIGURE_LOCATION_INTERVAL)) {
            return;
        }
        if (isDue(device.getId(), DeviceActionType.MEASURE_HEART_RATE,
                Duration.ofMinutes(properties.getHeartRateIntervalMinutes()), now)
                && tryCreate(device, DeviceActionType.MEASURE_HEART_RATE)) {
            return;
        }
        if (isDue(device.getId(), DeviceActionType.MEASURE_TEMPERATURE,
                Duration.ofMinutes(properties.getTemperatureIntervalMinutes()), now)
                && hasRecentValidHeartRate(device.getId(), now)) {
            tryCreate(device, DeviceActionType.MEASURE_TEMPERATURE);
        }
    }

    private boolean isBatteryLow(Device device) {
        return device.getBatteryLevel() != null
                && device.getBatteryLevel() < properties.getMinimumBatteryPercent();
    }

    private boolean isDue(Long deviceId, DeviceActionType type, Duration interval, Instant now) {
        return actionRepository.findFirstByDeviceIdAndActionTypeOrderByRequestedAtDesc(deviceId, type)
                .map(DeviceAction::getRequestedAt)
                .map(last -> !last.isAfter(now.minus(interval)))
                .orElse(true);
    }

    private boolean hasRecentValidHeartRate(Long deviceId, Instant now) {
        return healthRecordRepository.findFirstByDeviceIdAndHeartRateIsNotNullOrderByMeasuredAtDesc(deviceId)
                .filter(record -> record.getHeartRateStatus() == HealthMeasurementStatus.VALID)
                .map(HealthRecord::getMeasuredAt)
                .filter(measuredAt -> !measuredAt.isBefore(now.minus(
                        Duration.ofMinutes(properties.getTemperatureWearEvidenceMinutes()))))
                .isPresent();
    }

    private boolean tryCreate(Device device, DeviceActionType type) {
        DeviceActionService.Capability capability = actionService.capability(device, type);
        if (!capability.enabled()) return false;
        try {
            actionService.create(device.getId(), type, REQUESTED_BY);
            log.info("Automatic monitoring action sent: device={}, type={}", device.getDeviceNo(), type);
            return true;
        } catch (ResponseStatusException ex) {
            log.debug("Automatic monitoring action skipped: device={}, type={}, reason={}",
                    device.getDeviceNo(), type, ex.getReason());
            return false;
        } catch (RuntimeException ex) {
            log.warn("Automatic monitoring action failed: device={}, type={}", device.getDeviceNo(), type, ex);
            return false;
        }
    }
}
