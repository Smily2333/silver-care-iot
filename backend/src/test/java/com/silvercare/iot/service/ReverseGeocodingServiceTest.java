package com.silvercare.iot.service;

import com.silvercare.iot.config.GeocodingProperties;
import com.silvercare.iot.domain.AddressResolutionStatus;
import com.silvercare.iot.domain.entity.LocationAddressCache;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.geo.NominatimClient;
import com.silvercare.iot.repository.LocationAddressCacheRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReverseGeocodingServiceTest {

    private final LocationRecordRepository locationRepository = mock(LocationRecordRepository.class);
    private final LocationAddressCacheRepository cacheRepository = mock(LocationAddressCacheRepository.class);
    private final NominatimClient client = mock(NominatimClient.class);
    private final GeocodingProperties properties = new GeocodingProperties();

    @Test
    void invalidGpsIsSkippedWithoutCallingGeocoder() {
        LocationRecord record = record(false);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(record));
        properties.setEnabled(true);

        service().resolve(new LocationSavedEvent(1L));

        assertThat(record.getAddressStatus()).isEqualTo(AddressResolutionStatus.SKIPPED);
        verify(client, never()).reverse(any(), any());
    }

    @Test
    void reusesCachedAddressForNearbyPoint() {
        LocationRecord record = record(true);
        LocationAddressCache cache = new LocationAddressCache();
        cache.setApproximateAddress("天津市滨海新区第二大街");
        when(locationRepository.findById(1L)).thenReturn(Optional.of(record));
        when(cacheRepository.findById("39.0321:117.7008")).thenReturn(Optional.of(cache));
        properties.setEnabled(true);

        service().resolve(new LocationSavedEvent(1L));

        assertThat(record.getAddressStatus()).isEqualTo(AddressResolutionStatus.RESOLVED);
        assertThat(record.getApproximateAddress()).isEqualTo("天津市滨海新区第二大街");
        verify(client, never()).reverse(any(), any());
    }

    @Test
    void resolvesAndCachesAddress() {
        LocationRecord record = record(true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(record));
        when(cacheRepository.findById("39.0321:117.7008")).thenReturn(Optional.empty());
        when(client.reverse(any(), any())).thenReturn(Optional.of(new NominatimClient.AddressResult(
                "中国天津市滨海新区第二大街", "第二大街", "泰达街道", "滨海新区", "天津市")));
        properties.setEnabled(true);

        service().resolve(new LocationSavedEvent(1L));

        assertThat(record.getAddressStatus()).isEqualTo(AddressResolutionStatus.RESOLVED);
        assertThat(record.getApproximateAddress()).isEqualTo("天津市滨海新区泰达街道第二大街");
        verify(cacheRepository).save(any(LocationAddressCache.class));
    }

    private ReverseGeocodingService service() {
        return new ReverseGeocodingService(locationRepository, cacheRepository, client, properties);
    }

    private LocationRecord record(boolean gpsValid) {
        LocationRecord record = new LocationRecord();
        record.setGpsValid(gpsValid);
        record.setLatitude(new BigDecimal("39.032137"));
        record.setLongitude(new BigDecimal("117.7007781"));
        return record;
    }
}
