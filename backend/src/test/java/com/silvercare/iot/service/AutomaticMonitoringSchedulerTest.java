package com.silvercare.iot.service;

import com.silvercare.iot.config.AutomaticMonitoringProperties;
import com.silvercare.iot.domain.DeviceActionType;
import com.silvercare.iot.domain.DeviceStatus;
import com.silvercare.iot.domain.HealthMeasurementStatus;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.DeviceAction;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.repository.DeviceActionRepository;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomaticMonitoringSchedulerTest {
    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final DeviceActionRepository actionRepository = mock(DeviceActionRepository.class);
    private final HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
    private final DeviceActionService actionService = mock(DeviceActionService.class);
    private final AutomaticMonitoringProperties properties = new AutomaticMonitoringProperties();
    private final AutomaticMonitoringScheduler scheduler = new AutomaticMonitoringScheduler(
            deviceRepository, actionRepository, healthRecordRepository, actionService, properties);

    @Test
    void disabledMonitoringDoesNotScanDevices() {
        scheduler.runCycle();
        verify(deviceRepository, never()).findAll();
    }

    @Test
    void appliesMovingLocationIntervalBeforeHealthMeasurements() {
        properties.setEnabled(true);
        Device device = device();
        when(deviceRepository.findAll()).thenReturn(List.of(device));
        when(actionService.capability(device, DeviceActionType.CONFIGURE_LOCATION_INTERVAL))
                .thenReturn(new DeviceActionService.Capability(
                        DeviceActionType.CONFIGURE_LOCATION_INTERVAL, true, null));

        scheduler.runCycle();

        verify(actionService).create(1L, DeviceActionType.CONFIGURE_LOCATION_INTERVAL, "AUTO_MONITOR");
        verify(actionService, never()).create(1L, DeviceActionType.MEASURE_HEART_RATE, "AUTO_MONITOR");
    }

    @Test
    void temperatureRequiresRecentValidHeartRateEvidence() {
        properties.setEnabled(true);
        Device device = device();
        DeviceAction recentLocationConfig = action(DeviceActionType.CONFIGURE_LOCATION_INTERVAL);
        DeviceAction recentHeartMeasurement = action(DeviceActionType.MEASURE_HEART_RATE);
        HealthRecord validHeart = new HealthRecord();
        validHeart.setHeartRate(72);
        validHeart.setHeartRateStatus(HealthMeasurementStatus.VALID);

        when(deviceRepository.findAll()).thenReturn(List.of(device));
        when(actionRepository.findFirstByDeviceIdAndActionTypeOrderByRequestedAtDesc(
                1L, DeviceActionType.CONFIGURE_LOCATION_INTERVAL)).thenReturn(Optional.of(recentLocationConfig));
        when(actionRepository.findFirstByDeviceIdAndActionTypeOrderByRequestedAtDesc(
                1L, DeviceActionType.MEASURE_HEART_RATE)).thenReturn(Optional.of(recentHeartMeasurement));
        when(healthRecordRepository.findFirstByDeviceIdAndHeartRateIsNotNullOrderByMeasuredAtDesc(1L))
                .thenReturn(Optional.of(validHeart));
        when(actionService.capability(device, DeviceActionType.MEASURE_TEMPERATURE))
                .thenReturn(new DeviceActionService.Capability(DeviceActionType.MEASURE_TEMPERATURE, true, null));

        scheduler.runCycle();

        verify(actionService).create(1L, DeviceActionType.MEASURE_TEMPERATURE, "AUTO_MONITOR");
    }

    @Test
    void lowBatterySkipsAllAutomaticCommands() {
        properties.setEnabled(true);
        Device device = device();
        device.setBatteryLevel(19);
        when(deviceRepository.findAll()).thenReturn(List.of(device));

        scheduler.runCycle();

        verify(actionService, never()).create(any(), any(), any());
    }

    private DeviceAction action(DeviceActionType type) {
        DeviceAction action = new DeviceAction();
        action.setDeviceId(1L);
        action.setActionType(type);
        return action;
    }

    private Device device() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        device.setStatus(DeviceStatus.ONLINE);
        try {
            var field = Device.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(device, 1L);
            return device;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
