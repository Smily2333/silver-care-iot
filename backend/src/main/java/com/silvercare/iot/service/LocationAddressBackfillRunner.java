package com.silvercare.iot.service;

import com.silvercare.iot.config.GeocodingProperties;
import com.silvercare.iot.domain.AddressResolutionStatus;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class LocationAddressBackfillRunner implements ApplicationRunner {

    private final LocationRecordRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final GeocodingProperties properties;

    public LocationAddressBackfillRunner(LocationRecordRepository repository,
                                         ApplicationEventPublisher eventPublisher,
                                         GeocodingProperties properties) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || properties.getBackfillLimit() <= 0) {
            return;
        }
        repository.findGeocodingCandidates(
                        java.util.EnumSet.of(AddressResolutionStatus.PENDING, AddressResolutionStatus.FAILED),
                        PageRequest.of(0, properties.getBackfillLimit()))
                .forEach(record -> eventPublisher.publishEvent(new LocationSavedEvent(record.getId())));
    }
}
