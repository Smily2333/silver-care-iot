package com.silvercare.iot.api.dto;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;

public record MiniappOverviewResponse(
        Device device,
        HealthRecord latestHealth,
        LocationRecord latestLocation
) {
    public static MiniappOverviewResponse of(Device device,
                                              HealthRecord latestHealth,
                                              LocationRecord latestLocation) {
        return new MiniappOverviewResponse(device, latestHealth, latestLocation);
    }
}
