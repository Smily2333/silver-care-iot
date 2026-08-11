package com.silvercare.iot.service;

import com.silvercare.iot.config.DeviceActionProperties;
import com.silvercare.iot.domain.DeviceActionStatus;
import com.silvercare.iot.domain.DeviceActionType;
import com.silvercare.iot.domain.DeviceStatus;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.DeviceAction;
import com.silvercare.iot.protocol.ProtocolParser;
import com.silvercare.iot.repository.DeviceActionRepository;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.tcp.DeviceConnection;
import com.silvercare.iot.tcp.DeviceConnectionRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
public class DeviceActionService {
    private static final EnumSet<DeviceActionStatus> ACTIVE = EnumSet.of(
            DeviceActionStatus.PENDING, DeviceActionStatus.SENT, DeviceActionStatus.ACKNOWLEDGED);

    private final DeviceActionRepository repository;
    private final DeviceRepository deviceRepository;
    private final DeviceConnectionRegistry registry;
    private final DeviceCommandCatalog catalog;
    private final DeviceActionProperties properties;
    private final ProtocolParser parser = new ProtocolParser();

    public DeviceActionService(DeviceActionRepository repository, DeviceRepository deviceRepository,
                               DeviceConnectionRegistry registry, DeviceCommandCatalog catalog,
                               DeviceActionProperties properties) {
        this.repository = repository;
        this.deviceRepository = deviceRepository;
        this.registry = registry;
        this.catalog = catalog;
        this.properties = properties;
    }

    @Transactional
    public DeviceAction create(Long deviceId, DeviceActionType type, String requestedBy) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        Capability capability = capability(device, type);
        if (!capability.enabled()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, capability.reason());
        }
        repository.findFirstByDeviceIdAndActionTypeAndStatusInOrderByRequestedAtDesc(deviceId, type, ACTIVE)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "同类型操作正在执行中");
                });
        DeviceCommandCatalog.Definition definition = catalog.find(type).orElseThrow();
        DeviceAction action = new DeviceAction();
        action.setDeviceId(deviceId);
        action.setActionType(type);
        action.setCommandName(definition.commandName());
        action.setCommandContent(definition.content());
        action.setRequestedBy(requestedBy);
        action.setDeadlineAt(Instant.now().plus(definition.timeout()));
        action = repository.save(action);
        try {
            DeviceConnection connection = registry.find(device.getDeviceNo()).orElseThrow();
            connection.send(parser.build("3G", device.getDeviceNo(), definition.content()));
            action.setStatus(DeviceActionStatus.SENT);
            action.setSentAt(Instant.now());
        } catch (IOException | RuntimeException ex) {
            action.setStatus(DeviceActionStatus.SEND_FAILED);
            action.setFailureReason("命令写入设备连接失败");
            action.setCompletedAt(Instant.now());
        }
        return repository.save(action);
    }

    public Capability capability(Device device, DeviceActionType type) {
        if (catalog.find(type).isEmpty()) return new Capability(type, false, "通信流程待真机确认");
        if (!catalog.isConfirmed(type)) return new Capability(type, false, "通信指令待人工确认后启用");
        if (device.getStatus() != DeviceStatus.ONLINE) return new Capability(type, false, "设备离线");
        DeviceConnection connection = registry.find(device.getDeviceNo()).orElse(null);
        if (connection == null) return new Capability(type, false, "设备没有活动连接");
        if (connection.getLastSeenAt().isBefore(Instant.now().minusSeconds(properties.getConnectionFreshnessSeconds()))) {
            return new Capability(type, false, "设备连接状态已过期");
        }
        if ((type == DeviceActionType.MEASURE_HEART_RATE || type == DeviceActionType.MEASURE_TEMPERATURE)
                && !catalog.healthWearGateConfirmed()) {
            return new Capability(type, false, "佩戴状态位待确认，暂不主动测量");
        }
        return new Capability(type, true, null);
    }

    public List<Capability> capabilities(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return java.util.Arrays.stream(DeviceActionType.values()).map(type -> capability(device, type)).toList();
    }

    @Transactional
    public void acknowledge(Device device, String commandName) {
        repository.findFirstByDeviceIdAndCommandNameAndStatusInOrderByRequestedAtDesc(
                device.getId(), commandName, EnumSet.of(DeviceActionStatus.SENT))
                .ifPresent(action -> {
                    action.setStatus(DeviceActionStatus.ACKNOWLEDGED);
                    action.setAcknowledgedAt(Instant.now());
                });
    }

    @Transactional
    public void complete(Long deviceId, DeviceActionType type, String recordType, Long recordId, boolean valid) {
        repository.findFirstByDeviceIdAndActionTypeAndStatusInOrderByRequestedAtDesc(deviceId, type, ACTIVE)
                .ifPresent(action -> {
                    action.setAckMissing(action.getAcknowledgedAt() == null);
                    action.setStatus(valid ? DeviceActionStatus.COMPLETED : DeviceActionStatus.PARTIAL_SUCCESS);
                    action.setFailureReason(valid ? null : "设备返回了无效或异常测量值");
                    action.setResultRecordType(recordType);
                    action.setResultRecordId(recordId);
                    action.setCompletedAt(Instant.now());
                });
    }

    public DeviceAction require(Long deviceId, Long actionId) {
        DeviceAction action = repository.findById(actionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found"));
        if (!action.getDeviceId().equals(deviceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found");
        }
        return action;
    }

    public List<DeviceAction> list(Long deviceId) {
        deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return repository.findTop20ByDeviceIdOrderByRequestedAtDesc(deviceId);
    }

    @Transactional
    public void expireOverdue() {
        repository.findByDeadlineAtBeforeAndStatusIn(Instant.now(), ACTIVE).forEach(action -> {
            action.setStatus(action.getAcknowledgedAt() == null
                    ? DeviceActionStatus.ACK_TIMEOUT : DeviceActionStatus.RESULT_TIMEOUT);
            action.setFailureReason(action.getAcknowledgedAt() == null ? "设备确认超时" : "测量或定位结果超时");
            action.setCompletedAt(Instant.now());
        });
    }

    public record Capability(DeviceActionType type, boolean enabled, String reason) {}
}
