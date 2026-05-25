package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.RawPacketLog;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.protocol.ProtocolParseException;
import com.silvercare.iot.protocol.ProtocolParser;
import com.silvercare.iot.tcp.DeviceConnection;
import com.silvercare.iot.tcp.DeviceConnectionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DevicePacketDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DevicePacketDispatcher.class);

    private final ProtocolParser parser = new ProtocolParser();
    private final DeviceConnectionRegistry registry;
    private final DeviceService deviceService;
    private final RawPacketLogService rawPacketLogService;
    private final HealthDataService healthDataService;
    private final LocationDataService locationDataService;

    public DevicePacketDispatcher(DeviceConnectionRegistry registry,
                                  DeviceService deviceService,
                                  RawPacketLogService rawPacketLogService,
                                  HealthDataService healthDataService,
                                  LocationDataService locationDataService) {
        this.registry = registry;
        this.deviceService = deviceService;
        this.rawPacketLogService = rawPacketLogService;
        this.healthDataService = healthDataService;
        this.locationDataService = locationDataService;
    }

    public void dispatch(String rawPacket, DeviceConnection connection) {
        try {
            ProtocolFrame frame = parser.parse(rawPacket);
            connection.setDeviceNo(frame.deviceNo());
            registry.register(frame.deviceNo(), connection);
            Device device = deviceService.ensureOnline(frame.deviceNo());
            RawPacketLog packetLog = rawPacketLogService.saveSuccess(frame);

            switch (frame.command()) {
                case "LK" -> handleHeartbeat(frame, connection);
                case "btemp2" -> healthDataService.saveTemperature(device, frame, packetLog.getId());
                case "bphrt" -> healthDataService.saveBloodPressureHeartRate(device, frame, packetLog.getId());
                case "UD", "UD2", "AL", "UD_LTE", "UD_WCDMA", "UD_TDSCDMA", "UD_CDMA", "AL_LTE", "AL_WCDMA", "AL_TDSCDMA", "AL_CDMA" ->
                        locationDataService.saveLocation(device, frame, packetLog.getId());
                default -> log.info("Packet command ignored for MVP: {}", frame.command());
            }
        } catch (ProtocolParseException ex) {
            rawPacketLogService.saveFailure(rawPacket, ex.getMessage());
            log.warn("Failed to parse device packet: {}", ex.getMessage());
        }
    }

    public void onConnectionClosed(DeviceConnection connection) {
        if (connection.getDeviceNo() != null) {
            deviceService.markOffline(connection.getDeviceNo());
        }
    }

    private void handleHeartbeat(ProtocolFrame frame, DeviceConnection connection) {
        String[] args = frame.content().split(",");
        Integer steps = parseInt(args, 1);
        Integer battery = parseInt(args, 3);
        deviceService.updateHeartbeat(frame.deviceNo(), steps, battery);
        String reply = parser.build(frame.vendor(), frame.deviceNo(), "LK");
        try {
            connection.send(reply);
        } catch (IOException ex) {
            log.warn("Failed to reply heartbeat to device {}", frame.deviceNo(), ex);
        }
    }

    private Integer parseInt(String[] args, int index) {
        if (index >= args.length) {
            return null;
        }
        try {
            return Integer.valueOf(args[index].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
