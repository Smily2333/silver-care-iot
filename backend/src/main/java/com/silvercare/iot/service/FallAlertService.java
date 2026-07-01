package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.FallAlertRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

@Service
public class FallAlertService {

    private static final DateTimeFormatter DATE_FMT = new DateTimeFormatterBuilder()
            .appendPattern("ddMM")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .toFormatter();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private final FallAlertRepository repository;

    public FallAlertService(FallAlertRepository repository) {
        this.repository = repository;
    }

    public void saveAlert(Device device, ProtocolFrame frame, LocationRecord locationRecord, Long rawPacketId) {
        String[] args = frame.content().split(",");
        FallAlert alert = new FallAlert();
        alert.setDeviceId(device.getId());
        alert.setRawPacketId(rawPacketId);
        alert.setGpsValid("A".equalsIgnoreCase(value(args, 3)));
        alert.setLatitude(parseCoordinate(args, 4, 5));
        alert.setLongitude(parseCoordinate(args, 6, 7));
        if (locationRecord != null) {
            alert.setLocationRecordId(locationRecord.getId());
        }
        Instant alertedAt = parseAlertedAt(args);
        alert.setAlertedAt(alertedAt != null ? alertedAt : Instant.now());
        // TODO: trigger wx subscribe message push
        repository.save(alert);
    }

    private Instant parseAlertedAt(String[] args) {
        try {
            String date = value(args, 1);
            String time = value(args, 2);
            if (date == null || time == null || date.isBlank() || time.isBlank()) return null;
            return LocalDate.parse(date, DATE_FMT)
                    .atTime(LocalTime.parse(time, TIME_FMT))
                    .atZone(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
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
