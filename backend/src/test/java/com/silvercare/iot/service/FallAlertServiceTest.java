package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.domain.entity.RawPacketLog;
import com.silvercare.iot.protocol.ProtocolParser;
import com.silvercare.iot.repository.FallAlertRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FallAlertServiceTest {

    private final FallAlertRepository repository = mock(FallAlertRepository.class);
    private final FallAlertService service = new FallAlertService(repository);
    private final ProtocolParser parser = new ProtocolParser();

    @Test
    void saveAlert_parsesLocationAndTime() throws Exception {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        LocationRecord locationRecord = new LocationRecord();
        locationRecord.setDeviceId(1L);

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*0055*AL,120118,070625,A,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                locationRecord,
                packetLog(99L, Instant.parse("2018-01-12T07:06:26Z"))
        );

        ArgumentCaptor<FallAlert> captor = ArgumentCaptor.forClass(FallAlert.class);
        verify(repository).save(captor.capture());
        FallAlert alert = captor.getValue();

        assertThat(alert.getLatitude()).isEqualByComparingTo(new BigDecimal("22.570720"));
        assertThat(alert.getLongitude()).isEqualByComparingTo(new BigDecimal("113.8620167"));
        assertThat(alert.getGpsValid()).isTrue();
        assertThat(alert.getRawPacketId()).isEqualTo(99L);
        assertThat(alert.getAlertedAt()).isEqualTo(
                LocalDate.of(2018, 1, 12).atTime(LocalTime.of(7, 6, 25))
                        .atZone(ZoneOffset.UTC).toInstant());
    }

    @Test
    void saveAlert_gpsInvalid_setsGpsValidFalse() throws Exception {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*0055*AL,120118,070625,V,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                null,
                packetLog(1L, Instant.parse("2018-01-12T07:06:26Z"))
        );

        ArgumentCaptor<FallAlert> captor = ArgumentCaptor.forClass(FallAlert.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getGpsValid()).isFalse();
    }

    @Test
    void saveAlert_appliesSouthAndWestHemisphereSigns() throws Exception {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*0055*AL,120118,070625,A,22.570720,S,113.8620167,W,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                null,
                packetLog(1L, Instant.parse("2018-01-12T07:06:26Z"))
        );

        ArgumentCaptor<FallAlert> captor = ArgumentCaptor.forClass(FallAlert.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getLatitude()).isEqualByComparingTo(new BigDecimal("-22.570720"));
        assertThat(captor.getValue().getLongitude()).isEqualByComparingTo(new BigDecimal("-113.8620167"));
    }

    @Test
    void saveAlert_replacesImplausibleFutureTimeWithPacketReceivedAt() throws Exception {
        Device device = new Device();
        Instant receivedAt = Instant.parse("2026-07-03T10:19:49Z");

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*0051*AL,070236,074134,V,39.032137,N,117.7007781,E,0.00,0.0,-23.9,1,77,100,0,0,00100000]"),
                null,
                packetLog(2L, receivedAt)
        );

        ArgumentCaptor<FallAlert> captor = ArgumentCaptor.forClass(FallAlert.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAlertedAt()).isEqualTo(receivedAt);
    }

    private RawPacketLog packetLog(Long id, Instant receivedAt) throws Exception {
        RawPacketLog packetLog = new RawPacketLog();
        var idField = RawPacketLog.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(packetLog, id);
        packetLog.setReceivedAt(receivedAt);
        return packetLog;
    }
}
