package com.silvercare.iot.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTimestampResolverTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-07-03T10:19:49Z");

    @Test
    void keepsPlausibleDeviceTimestamp() {
        assertThat(DeviceTimestampResolver.resolve("030726", "101948", RECEIVED_AT))
                .isEqualTo(Instant.parse("2026-07-03T10:19:48Z"));
    }

    @Test
    void replacesImplausibleFutureTimestampWithReceivedAt() {
        assertThat(DeviceTimestampResolver.resolve("070236", "074134", RECEIVED_AT))
                .isEqualTo(RECEIVED_AT);
    }

    @Test
    void replacesImplausiblePastTimestampWithReceivedAt() {
        assertThat(DeviceTimestampResolver.resolve("120118", "070625", RECEIVED_AT))
                .isEqualTo(RECEIVED_AT);
    }

    @Test
    void replacesMalformedTimestampWithReceivedAt() {
        assertThat(DeviceTimestampResolver.resolve("not-a-date", "bad", RECEIVED_AT))
                .isEqualTo(RECEIVED_AT);
    }
}
