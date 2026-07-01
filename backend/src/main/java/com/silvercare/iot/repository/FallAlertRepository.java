package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.FallAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FallAlertRepository extends JpaRepository<FallAlert, Long> {

    Optional<FallAlert> findFirstByDeviceIdOrderByAlertedAtDesc(Long deviceId);

    List<FallAlert> findTop20ByDeviceIdOrderByAlertedAtDesc(Long deviceId);
}
