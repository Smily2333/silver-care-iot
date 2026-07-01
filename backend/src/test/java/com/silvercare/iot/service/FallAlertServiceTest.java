package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.protocol.ProtocolParser;
import com.silvercare.iot.repository.FallAlertRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
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
    void saveAlert_parsesLocationAndTime() {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        LocationRecord locationRecord = new LocationRecord();
        locationRecord.setDeviceId(1L);

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*0055*AL,120118,070625,A,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                locationRecord,
                99L
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
    void saveAlert_gpsInvalid_setsGpsValidFalse() {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*0055*AL,120118,070625,V,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                null,
                1L
        );

        ArgumentCaptor<FallAlert> captor = ArgumentCaptor.forClass(FallAlert.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getGpsValid()).isFalse();
    }

    @Test
    void saveAlert_appliesSouthAndWestHemisphereSigns() {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*0055*AL,120118,070625,A,22.570720,S,113.8620167,W,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                null,
                1L
        );

        ArgumentCaptor<FallAlert> captor = ArgumentCaptor.forClass(FallAlert.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getLatitude()).isEqualByComparingTo(new BigDecimal("-22.570720"));
        assertThat(captor.getValue().getLongitude()).isEqualByComparingTo(new BigDecimal("-113.8620167"));
    }
}
