package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.DeviceBinding;
import com.silvercare.iot.repository.DeviceBindingRepository;
import com.silvercare.iot.repository.DeviceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeviceAccessService {

    private final DeviceRepository deviceRepository;
    private final DeviceBindingRepository bindingRepository;

    public DeviceAccessService(DeviceRepository deviceRepository, DeviceBindingRepository bindingRepository) {
        this.deviceRepository = deviceRepository;
        this.bindingRepository = bindingRepository;
    }

    @Transactional(readOnly = true)
    public Device requireBoundDevice(Long userId, String deviceNo) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        if (!bindingRepository.existsByUserIdAndDeviceId(userId, device.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device is not bound to this user");
        }
        return device;
    }

    @Transactional
    public Device bind(Long userId, String deviceNo, String ownerName) {
        Device device = deviceRepository.findByDeviceNoForUpdate(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        bindingRepository.findByDeviceId(device.getId()).ifPresentOrElse(existing -> {
            if (!existing.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Device is already bound");
            }
        }, () -> {
            DeviceBinding binding = new DeviceBinding();
            binding.setUserId(userId);
            binding.setDeviceId(device.getId());
            bindingRepository.save(binding);
        });
        device.setOwnerName(ownerName);
        return deviceRepository.save(device);
    }
}
