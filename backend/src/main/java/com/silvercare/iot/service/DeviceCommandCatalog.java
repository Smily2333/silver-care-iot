package com.silvercare.iot.service;

import com.silvercare.iot.config.DeviceActionProperties;
import com.silvercare.iot.domain.DeviceActionType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class DeviceCommandCatalog {
    private final DeviceActionProperties properties;

    public DeviceCommandCatalog(DeviceActionProperties properties) {
        this.properties = properties;
    }

    public Optional<Definition> find(DeviceActionType type) {
        Definition definition = switch (type) {
            case LOCATE_NOW -> new Definition("CR", "CR", Duration.ofSeconds(properties.getLocateTimeoutSeconds()));
            case MEASURE_HEART_RATE -> new Definition("hrtstart", "hrtstart,1", Duration.ofSeconds(properties.getHealthTimeoutSeconds()));
            case MEASURE_TEMPERATURE -> new Definition("bodytemp2", "bodytemp2", Duration.ofSeconds(properties.getHealthTimeoutSeconds()));
            case MEASURE_HEALTH -> null; // TODO(protocol): 真机确认串行间隔和结果顺序后再开放组合检测。
        };
        return Optional.ofNullable(definition);
    }

    public boolean isConfirmed(DeviceActionType type) {
        return properties.getConfirmedTypes().contains(type);
    }

    public boolean healthWearGateConfirmed() {
        return properties.isAllowHealthWithoutWearStatus();
    }

    public record Definition(String commandName, String content, Duration timeout) {}
}
