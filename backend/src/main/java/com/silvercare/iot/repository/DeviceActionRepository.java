package com.silvercare.iot.repository;

import com.silvercare.iot.domain.DeviceActionStatus;
import com.silvercare.iot.domain.DeviceActionType;
import com.silvercare.iot.domain.entity.DeviceAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceActionRepository extends JpaRepository<DeviceAction, Long> {
    List<DeviceAction> findTop20ByDeviceIdOrderByRequestedAtDesc(Long deviceId);
    Optional<DeviceAction> findFirstByDeviceIdAndActionTypeAndStatusInOrderByRequestedAtDesc(
            Long deviceId, DeviceActionType actionType, Collection<DeviceActionStatus> statuses);
    Optional<DeviceAction> findFirstByDeviceIdAndCommandNameAndStatusInOrderByRequestedAtDesc(
            Long deviceId, String commandName, Collection<DeviceActionStatus> statuses);
    List<DeviceAction> findByDeadlineAtBeforeAndStatusIn(Instant deadline, Collection<DeviceActionStatus> statuses);
}
