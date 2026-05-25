package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceNo(String deviceNo);
}
