package com.silvercare.iot.config;

import com.silvercare.iot.domain.DeviceActionType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "silver-care.device-actions")
public class DeviceActionProperties {
    private Set<DeviceActionType> confirmedTypes = new HashSet<>();
    private boolean allowHealthWithoutWearStatus = false;
    private int connectionFreshnessSeconds = 180;
    private int locateTimeoutSeconds = 120;
    private int healthTimeoutSeconds = 180;

    public Set<DeviceActionType> getConfirmedTypes() { return confirmedTypes; }
    public void setConfirmedTypes(Set<DeviceActionType> value) { confirmedTypes = value; }
    public boolean isAllowHealthWithoutWearStatus() { return allowHealthWithoutWearStatus; }
    public void setAllowHealthWithoutWearStatus(boolean value) { allowHealthWithoutWearStatus = value; }
    public int getConnectionFreshnessSeconds() { return connectionFreshnessSeconds; }
    public void setConnectionFreshnessSeconds(int value) { connectionFreshnessSeconds = value; }
    public int getLocateTimeoutSeconds() { return locateTimeoutSeconds; }
    public void setLocateTimeoutSeconds(int value) { locateTimeoutSeconds = value; }
    public int getHealthTimeoutSeconds() { return healthTimeoutSeconds; }
    public void setHealthTimeoutSeconds(int value) { healthTimeoutSeconds = value; }
}
