package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.Device;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceNo(String deviceNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Device d where d.deviceNo = :deviceNo")
    Optional<Device> findByDeviceNoForUpdate(@Param("deviceNo") String deviceNo);
}
