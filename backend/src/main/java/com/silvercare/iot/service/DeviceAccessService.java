package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.DeviceBinding;
import com.silvercare.iot.repository.DeviceBindingRepository;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.MiniappUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeviceAccessService {

    public static final long MAX_DEVICES_PER_USER = 4;
    public static final long MAX_USERS_PER_DEVICE = 4;

    private final DeviceRepository deviceRepository;
    private final DeviceBindingRepository bindingRepository;
    private final MiniappUserRepository miniappUserRepository;

    public DeviceAccessService(DeviceRepository deviceRepository,
                               DeviceBindingRepository bindingRepository,
                               MiniappUserRepository miniappUserRepository) {
        this.deviceRepository = deviceRepository;
        this.bindingRepository = bindingRepository;
        this.miniappUserRepository = miniappUserRepository;
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
        // 固定按“用户 -> 设备”的顺序加锁，分别串行化同一用户和同一设备的并发绑定。
        miniappUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Miniapp user not found"));
        Device device = deviceRepository.findByDeviceNoForUpdate(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        if (bindingRepository.findByUserIdAndDeviceId(userId, device.getId()).isEmpty()) {
            if (bindingRepository.countByUserId(userId) >= MAX_DEVICES_PER_USER) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A miniapp user can bind at most 4 devices");
            }
            if (bindingRepository.countByDeviceId(device.getId()) >= MAX_USERS_PER_DEVICE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A device can be bound by at most 4 miniapp users");
            }
            DeviceBinding binding = new DeviceBinding();
            binding.setUserId(userId);
            binding.setDeviceId(device.getId());
            bindingRepository.save(binding);
        }
        device.setOwnerName(ownerName);
        return deviceRepository.save(device);
    }
}
