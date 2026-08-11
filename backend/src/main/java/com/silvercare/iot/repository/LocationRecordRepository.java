package com.silvercare.iot.repository;

import com.silvercare.iot.domain.AddressResolutionStatus;
import com.silvercare.iot.domain.entity.LocationRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface LocationRecordRepository extends JpaRepository<LocationRecord, Long> {

    Optional<LocationRecord> findFirstByDeviceIdAndLocatedAtBeforeOrderByLocatedAtDesc(Long deviceId, Instant cutoff);

    List<LocationRecord> findTop100ByDeviceIdAndLocatedAtBeforeOrderByLocatedAtDesc(Long deviceId, Instant cutoff);

    List<LocationRecord> findByDeviceIdAndGpsValidTrueAndLocatedAtBeforeOrderByLocatedAtDesc(
            Long deviceId, Instant cutoff, Pageable pageable);

    @Query("""
            select location from LocationRecord location
            where location.gpsValid = true
              and (location.addressStatus is null or location.addressStatus in :retryStatuses)
            order by location.locatedAt desc
            """)
    List<LocationRecord> findGeocodingCandidates(
            @Param("retryStatuses") Collection<AddressResolutionStatus> retryStatuses,
            Pageable pageable);
}
