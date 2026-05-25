package com.silvercare.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "silver-care.gateway")
public class DeviceGatewayProperties {

    private boolean enabled = true;
    private int port = 9000;
    private int idleTimeoutSeconds = 180;
    private int maxClientThreads = 200;
    private int clientQueueCapacity = 200;
    private int acceptBacklog = 128;
    private int maxFrameLengthBytes = 8192;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public int getMaxClientThreads() {
        return maxClientThreads;
    }

    public void setMaxClientThreads(int maxClientThreads) {
        this.maxClientThreads = maxClientThreads;
    }

    public int getClientQueueCapacity() {
        return clientQueueCapacity;
    }

    public void setClientQueueCapacity(int clientQueueCapacity) {
        this.clientQueueCapacity = clientQueueCapacity;
    }

    public int getAcceptBacklog() {
        return acceptBacklog;
    }

    public void setAcceptBacklog(int acceptBacklog) {
        this.acceptBacklog = acceptBacklog;
    }

    public int getMaxFrameLengthBytes() {
        return maxFrameLengthBytes;
    }

    public void setMaxFrameLengthBytes(int maxFrameLengthBytes) {
        this.maxFrameLengthBytes = maxFrameLengthBytes;
    }
}
