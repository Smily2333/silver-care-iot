package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.LocationAddressCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationAddressCacheRepository extends JpaRepository<LocationAddressCache, String> {
}
