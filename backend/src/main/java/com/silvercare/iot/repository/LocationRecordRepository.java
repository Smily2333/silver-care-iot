package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.LocationRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRecordRepository extends JpaRepository<LocationRecord, Long> {

    Optional<LocationRecord> findFirstByDeviceIdOrderByLocatedAtDesc(Long deviceId);

    List<LocationRecord> findTop100ByDeviceIdOrderByLocatedAtDesc(Long deviceId);

    List<LocationRecord> findByDeviceIdAndGpsValidTrueOrderByLocatedAtDesc(Long deviceId, Pageable pageable);
}
