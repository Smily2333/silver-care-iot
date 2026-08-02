package com.silvercare.iot.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

final class DeviceTimestampResolver {

    static final Duration MAX_ACCEPTABLE_SKEW = Duration.ofDays(30);

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("ddMM")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .toFormatter();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private DeviceTimestampResolver() {
    }

    static Instant resolve(String date, String time, Instant receivedAt) {
        Instant fallback = receivedAt != null ? receivedAt : Instant.now();
        if (date == null || time == null || date.isBlank() || time.isBlank()) {
            return fallback;
        }

        try {
            Instant deviceTimestamp = LocalDate.parse(date, DATE_FORMATTER)
                    .atTime(LocalTime.parse(time, TIME_FORMATTER))
                    .atZone(ZoneOffset.UTC)
                    .toInstant();
            Duration skew = Duration.between(fallback, deviceTimestamp).abs();
            return skew.compareTo(MAX_ACCEPTABLE_SKEW) <= 0 ? deviceTimestamp : fallback;
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }
}
