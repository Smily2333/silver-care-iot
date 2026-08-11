package com.silvercare.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "silver-care.geocoding")
public class GeocodingProperties {

    private boolean enabled = false;
    private String baseUrl = "http://127.0.0.1:7070";
    private int connectTimeoutMillis = 2000;
    private int readTimeoutMillis = 4000;
    private String userAgent = "SilverCare/0.1";
    private int backfillLimit = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getBackfillLimit() {
        return backfillLimit;
    }

    public void setBackfillLimit(int backfillLimit) {
        this.backfillLimit = backfillLimit;
    }
}
