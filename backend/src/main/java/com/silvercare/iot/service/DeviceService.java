package com.silvercare.iot.service;

import com.silvercare.iot.domain.DeviceStatus;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public Device ensureOnline(String deviceNo) {
        Device device = deviceRepository.findByDeviceNo(deviceNo).orElseGet(() -> {
            Device created = new Device();
            created.setDeviceNo(deviceNo);
            return created;
        });
        Instant now = Instant.now();
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastOnlineAt(now);
        return deviceRepository.save(device);
    }

    @Transactional
    public Device updateHeartbeat(String deviceNo, Integer steps, Integer batteryLevel) {
        Device device = ensureOnline(deviceNo);
        device.setLastHeartbeatAt(Instant.now());
        device.setStepCount(steps);
        device.setBatteryLevel(batteryLevel);
        return deviceRepository.save(device);
    }

    @Transactional
    public void markOffline(String deviceNo) {
        deviceRepository.findByDeviceNo(deviceNo).ifPresent(device -> {
            device.setStatus(DeviceStatus.OFFLINE);
            deviceRepository.save(device);
        });
    }
}
