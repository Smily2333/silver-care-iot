package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.protocol.ProtocolParser;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class LocationDataServiceTest {

    @Test
    void savesKnownLocationFields() {
        LocationRecordRepository repository = mock(LocationRecordRepository.class);
        LocationDataService service = new LocationDataService(repository);
        ProtocolParser parser = new ProtocolParser();
        Device device = new Device();
        device.setDeviceNo("2016001000");

        when(repository.save(any(LocationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationRecord result = service.saveLocation(
                device,
                parser.parse("[3G*2016001000*00E0*UD,120118,070625,A,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010,6,255,460,0,9360,5081,156,9360,4081,129,9360,4151,128,9360,5082,127,9360,4723,122,9360,4082,120,5,buyaoxialian,a0:c5:f2:b0:7.4:d0,-34,22.4]"),
                1L
        );

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualByComparingTo(new BigDecimal("22.570720"));
        assertThat(result.getLongitude()).isEqualByComparingTo(new BigDecimal("113.8620167"));
        assertThat(result.getLocatedAt()).isEqualTo(LocalDate.of(2018, 1, 12)
                .atTime(LocalTime.of(7, 6, 25))
                .atZone(ZoneOffset.UTC)
                .toInstant());
    }

    @Test
    void savesSouthAndWestCoordinatesAsNegative() {
        LocationRecordRepository repository = mock(LocationRecordRepository.class);
        LocationDataService service = new LocationDataService(repository);
        ProtocolParser parser = new ProtocolParser();
        Device device = new Device();
        device.setDeviceNo("2016001000");

        when(repository.save(any(LocationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationRecord result = service.saveLocation(
                device,
                parser.parse("[3G*2016001000*0055*UD,120118,070625,A,22.570720,S,113.8620167,W,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                1L
        );

        assertThat(result.getLatitude()).isEqualByComparingTo(new BigDecimal("-22.570720"));
        assertThat(result.getLongitude()).isEqualByComparingTo(new BigDecimal("-113.8620167"));
    }
}
