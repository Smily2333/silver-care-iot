package com.silvercare.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "silver-care.automatic-monitoring")
public class AutomaticMonitoringProperties {
    private boolean enabled;
    private long scanMillis = 60_000;
    private int heartRateIntervalMinutes = 60;
    private int temperatureIntervalMinutes = 240;
    private int temperatureWearEvidenceMinutes = 10;
    private int locationUploadSeconds = 600;
    private int locationReapplyHours = 24;
    private int minimumBatteryPercent = 20;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public long getScanMillis() { return scanMillis; }
    public void setScanMillis(long value) { scanMillis = value; }
    public int getHeartRateIntervalMinutes() { return heartRateIntervalMinutes; }
    public void setHeartRateIntervalMinutes(int value) { heartRateIntervalMinutes = value; }
    public int getTemperatureIntervalMinutes() { return temperatureIntervalMinutes; }
    public void setTemperatureIntervalMinutes(int value) { temperatureIntervalMinutes = value; }
    public int getTemperatureWearEvidenceMinutes() { return temperatureWearEvidenceMinutes; }
    public void setTemperatureWearEvidenceMinutes(int value) { temperatureWearEvidenceMinutes = value; }
    public int getLocationUploadSeconds() { return locationUploadSeconds; }
    public void setLocationUploadSeconds(int value) { locationUploadSeconds = value; }
    public int getLocationReapplyHours() { return locationReapplyHours; }
    public void setLocationReapplyHours(int value) { locationReapplyHours = value; }
    public int getMinimumBatteryPercent() { return minimumBatteryPercent; }
    public void setMinimumBatteryPercent(int value) { minimumBatteryPercent = value; }
}
