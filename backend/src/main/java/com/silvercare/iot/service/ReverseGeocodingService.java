package com.silvercare.iot.service;

import com.silvercare.iot.config.GeocodingProperties;
import com.silvercare.iot.domain.AddressResolutionStatus;
import com.silvercare.iot.domain.entity.LocationAddressCache;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.geo.NominatimClient;
import com.silvercare.iot.repository.LocationAddressCacheRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReverseGeocodingService {

    private final LocationRecordRepository locationRepository;
    private final LocationAddressCacheRepository cacheRepository;
    private final NominatimClient nominatimClient;
    private final GeocodingProperties properties;

    public ReverseGeocodingService(LocationRecordRepository locationRepository,
                                   LocationAddressCacheRepository cacheRepository,
                                   NominatimClient nominatimClient,
                                   GeocodingProperties properties) {
        this.locationRepository = locationRepository;
        this.cacheRepository = cacheRepository;
        this.nominatimClient = nominatimClient;
        this.properties = properties;
    }

    @Async("geocodingExecutor")
    @EventListener
    @Transactional
    public void resolve(LocationSavedEvent event) {
        LocationRecord record = locationRepository.findById(event.locationRecordId()).orElse(null);
        if (record == null || record.getAddressStatus() == AddressResolutionStatus.RESOLVED) {
            return;
        }
        if (!Boolean.TRUE.equals(record.getGpsValid()) || !validCoordinates(record)) {
            record.setAddressStatus(AddressResolutionStatus.SKIPPED);
            return;
        }
        if (!properties.isEnabled()) {
            return;
        }

        record.setAddressStatus(AddressResolutionStatus.PENDING);
        String gridKey = gridKey(record.getLatitude(), record.getLongitude());
        LocationAddressCache cached = cacheRepository.findById(gridKey).orElse(null);
        if (cached != null) {
            applyResolved(record, cached.getApproximateAddress());
            return;
        }

        try {
            NominatimClient.AddressResult result = nominatimClient
                    .reverse(record.getLatitude(), record.getLongitude())
                    .orElse(null);
            if (result == null) {
                record.setAddressStatus(AddressResolutionStatus.NOT_FOUND);
                record.setAddressResolvedAt(Instant.now());
                return;
            }
            String address = buildApproximateAddress(result);
            if (address == null || address.isBlank()) {
                record.setAddressStatus(AddressResolutionStatus.NOT_FOUND);
                record.setAddressResolvedAt(Instant.now());
                return;
            }
            cacheRepository.save(toCache(gridKey, record, result, address));
            applyResolved(record, address);
        } catch (RuntimeException ex) {
            record.setAddressStatus(AddressResolutionStatus.FAILED);
            record.setAddressResolvedAt(Instant.now());
        }
    }

    private boolean validCoordinates(LocationRecord record) {
        if (record.getLatitude() == null || record.getLongitude() == null) {
            return false;
        }
        return record.getLatitude().compareTo(BigDecimal.valueOf(-90)) >= 0
                && record.getLatitude().compareTo(BigDecimal.valueOf(90)) <= 0
                && record.getLongitude().compareTo(BigDecimal.valueOf(-180)) >= 0
                && record.getLongitude().compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    private String gridKey(BigDecimal latitude, BigDecimal longitude) {
        return latitude.setScale(4, RoundingMode.HALF_UP).toPlainString()
                + ":" + longitude.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private String buildApproximateAddress(NominatimClient.AddressResult result) {
        List<String> parts = new ArrayList<>();
        addDistinct(parts, result.city());
        addDistinct(parts, result.district());
        addDistinct(parts, result.neighbourhood());
        addDistinct(parts, result.road());
        if (parts.isEmpty()) {
            return result.displayName();
        }
        return String.join("", parts);
    }

    private void addDistinct(List<String> parts, String value) {
        if (value != null && !value.isBlank() && parts.stream().noneMatch(value::equals)) {
            parts.add(value);
        }
    }

    private LocationAddressCache toCache(String gridKey, LocationRecord record,
                                         NominatimClient.AddressResult result, String address) {
        LocationAddressCache cache = new LocationAddressCache();
        cache.setGridKey(gridKey);
        cache.setLatitude(record.getLatitude());
        cache.setLongitude(record.getLongitude());
        cache.setApproximateAddress(address);
        cache.setRoad(result.road());
        cache.setNeighbourhood(result.neighbourhood());
        cache.setDistrict(result.district());
        cache.setCity(result.city());
        cache.setDisplayName(result.displayName());
        cache.setUpdatedAt(Instant.now());
        return cache;
    }

    private void applyResolved(LocationRecord record, String address) {
        record.setApproximateAddress(address);
        record.setAddressStatus(AddressResolutionStatus.RESOLVED);
        record.setAddressResolvedAt(Instant.now());
    }
}
