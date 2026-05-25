package com.silvercare.iot.tcp;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DeviceConnectionRegistry {

    private final ConcurrentMap<String, DeviceConnection> connections = new ConcurrentHashMap<>();

    public void register(String deviceNo, DeviceConnection connection) {
        connection.setDeviceNo(deviceNo);
        DeviceConnection previous = connections.put(deviceNo, connection);
        if (previous != null && previous != connection) {
            previous.closeQuietly();
        }
    }

    public Optional<DeviceConnection> find(String deviceNo) {
        return Optional.ofNullable(connections.get(deviceNo));
    }

    public void remove(DeviceConnection connection) {
        String deviceNo = connection.getDeviceNo();
        if (deviceNo != null) {
            connections.remove(deviceNo, connection);
        }
    }
}
