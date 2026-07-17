package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.DeviceBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceBindingRepository extends JpaRepository<DeviceBinding, Long> {

    Optional<DeviceBinding> findByDeviceId(Long deviceId);

    boolean existsByUserIdAndDeviceId(Long userId, Long deviceId);
}
