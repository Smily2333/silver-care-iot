package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.domain.entity.RawPacketLog;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class LocationDataService {

    private final LocationRecordRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final DeviceActionService actionService;

    public LocationDataService(LocationRecordRepository repository, ApplicationEventPublisher eventPublisher,
                               DeviceActionService actionService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.actionService = actionService;
    }

    public LocationRecord saveLocation(Device device, ProtocolFrame frame, RawPacketLog packetLog) {
        String[] args = frame.content().split(",");
        LocationRecord record = new LocationRecord();
        record.setDeviceId(device.getId());
        record.setSourceCommand(frame.command());
        record.setRawPacketId(packetLog.getId());
        record.setLocatedAt(parseLocatedAt(args, packetLog.getReceivedAt()));
        record.setGpsValid("A".equalsIgnoreCase(value(args, 3)));
        record.setLatitude(parseCoordinate(args, 4, 5));
        record.setLatitudeHemisphere(value(args, 5));
        record.setLongitude(parseCoordinate(args, 6, 7));
        record.setLongitudeHemisphere(value(args, 7));
        record.setSpeed(parseDecimal(args, 8));
        record.setDirection(parseDecimal(args, 9));
        record.setAltitude(parseDecimal(args, 10));
        record.setSatelliteCount(parseInt(args, 11));
        record.setGsmSignal(parseInt(args, 12));
        record.setBatteryLevel(parseInt(args, 13));
        record.setStepCount(parseInt(args, 14));
        record.setRolloverCount(parseInt(args, 15));
        record.setTerminalStatus(value(args, 16));
        record.setAccuracy(parseLastDecimal(args));
        LocationRecord saved = repository.save(record);
        eventPublisher.publishEvent(new LocationSavedEvent(saved.getId()));
        if (Boolean.TRUE.equals(saved.getGpsValid())) {
            actionService.complete(device.getId(), com.silvercare.iot.domain.DeviceActionType.LOCATE_NOW,
                    "LOCATION", saved.getId(), true);
        }
        return saved;
    }

    private Instant parseLocatedAt(String[] args, Instant receivedAt) {
        return DeviceTimestampResolver.resolve(value(args, 1), value(args, 2), receivedAt);
    }

    private String value(String[] args, int index) {
        return index < args.length ? args[index].trim() : null;
    }

    private Integer parseInt(String[] args, int index) {
        try {
            String value = value(args, index);
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String[] args, int index) {
        try {
            String value = value(args, index);
            return value == null || value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseCoordinate(String[] args, int valueIndex, int hemisphereIndex) {
        BigDecimal coordinate = parseDecimal(args, valueIndex);
        String hemisphere = value(args, hemisphereIndex);
        if (coordinate == null || hemisphere == null) {
            return coordinate;
        }
        return switch (hemisphere.toUpperCase()) {
            case "S", "W" -> coordinate.negate();
            default -> coordinate;
        };
    }

    private BigDecimal parseLastDecimal(String[] args) {
        if (args.length == 0) {
            return null;
        }
        try {
            return new BigDecimal(args[args.length - 1].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
