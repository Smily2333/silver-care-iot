package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.domain.entity.RawPacketLog;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.FallAlertRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class FallAlertService {

    private final FallAlertRepository repository;

    public FallAlertService(FallAlertRepository repository) {
        this.repository = repository;
    }

    public void saveAlert(Device device, ProtocolFrame frame, LocationRecord locationRecord, RawPacketLog packetLog) {
        String[] args = frame.content().split(",");
        FallAlert alert = new FallAlert();
        alert.setDeviceId(device.getId());
        alert.setRawPacketId(packetLog.getId());
        alert.setGpsValid("A".equalsIgnoreCase(value(args, 3)));
        alert.setLatitude(parseCoordinate(args, 4, 5));
        alert.setLongitude(parseCoordinate(args, 6, 7));
        if (locationRecord != null) {
            alert.setLocationRecordId(locationRecord.getId());
        }
        alert.setAlertedAt(parseAlertedAt(args, packetLog.getReceivedAt()));
        // TODO: trigger wx subscribe message push
        repository.save(alert);
    }

    private Instant parseAlertedAt(String[] args, Instant receivedAt) {
        return DeviceTimestampResolver.resolve(value(args, 1), value(args, 2), receivedAt);
    }

    private String value(String[] args, int index) {
        return index < args.length ? args[index].trim() : null;
    }

    private BigDecimal parseDecimal(String[] args, int index) {
        try {
            String v = value(args, index);
            return v == null || v.isBlank() ? null : new BigDecimal(v);
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
}
