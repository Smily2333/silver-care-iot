package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

@Service
public class LocationDataService {

    private static final DateTimeFormatter DEVICE_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("ddMM")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .toFormatter();
    private static final DateTimeFormatter DEVICE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final LocationRecordRepository repository;

    public LocationDataService(LocationRecordRepository repository) {
        this.repository = repository;
    }

    public void saveLocation(Device device, ProtocolFrame frame, Long rawPacketId) {
        String[] args = frame.content().split(",");
        LocationRecord record = new LocationRecord();
        record.setDeviceId(device.getId());
        record.setSourceCommand(frame.command());
        record.setRawPacketId(rawPacketId);
        Instant locatedAt = parseLocatedAt(args);
        if (locatedAt != null) {
            record.setLocatedAt(locatedAt);
        }
        record.setGpsValid("A".equalsIgnoreCase(value(args, 3)));
        record.setLatitude(parseDecimal(args, 4));
        record.setLatitudeHemisphere(value(args, 5));
        record.setLongitude(parseDecimal(args, 6));
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
        repository.save(record);
    }

    private Instant parseLocatedAt(String[] args) {
        try {
            String date = value(args, 1);
            String time = value(args, 2);
            if (date == null || time == null || date.isBlank() || time.isBlank()) {
                return null;
            }
            return LocalDate.parse(date, DEVICE_DATE_FORMATTER)
                    .atTime(LocalTime.parse(time, DEVICE_TIME_FORMATTER))
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
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
