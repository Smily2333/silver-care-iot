package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.RawPacketLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawPacketLogRepository extends JpaRepository<RawPacketLog, Long> {
}
