package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    Optional<HealthRecord> findFirstByDeviceIdOrderByMeasuredAtDesc(Long deviceId);

    List<HealthRecord> findTop100ByDeviceIdOrderByMeasuredAtDesc(Long deviceId);
}
