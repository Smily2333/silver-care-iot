package com.silvercare.iot.api.dto;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;

public record MiniappOverviewResponse(
        Device device,
        HealthRecord latestHealth,
        LocationRecord latestLocation,
        AdminHealthSummaryResponse healthSummary
) {
    public static MiniappOverviewResponse of(Device device,
                                              HealthRecord latestHealth,
                                              LocationRecord latestLocation,
                                              AdminHealthSummaryResponse healthSummary) {
        return new MiniappOverviewResponse(device, latestHealth, latestLocation, healthSummary);
    }
}
