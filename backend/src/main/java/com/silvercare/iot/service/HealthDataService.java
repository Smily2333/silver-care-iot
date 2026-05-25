package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.HealthRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class HealthDataService {

    private final HealthRecordRepository repository;

    public HealthDataService(HealthRecordRepository repository) {
        this.repository = repository;
    }

    public void saveTemperature(Device device, ProtocolFrame frame, Long rawPacketId) {
        String[] args = split(frame.content());
        HealthRecord record = new HealthRecord();
        record.setDeviceId(device.getId());
        record.setSourceCommand(frame.command());
        record.setRawPacketId(rawPacketId);
        record.setTemperatureType(parseInt(args, 1));
        record.setBodyTemperature(parseDecimal(args, 2));
        repository.save(record);
    }

    public void saveBloodPressureHeartRate(Device device, ProtocolFrame frame, Long rawPacketId) {
        String[] args = split(frame.content());
        HealthRecord record = new HealthRecord();
        record.setDeviceId(device.getId());
        record.setSourceCommand(frame.command());
        record.setRawPacketId(rawPacketId);
        record.setSystolicPressure(parseInt(args, 1));
        record.setDiastolicPressure(parseInt(args, 2));
        record.setHeartRate(parseInt(args, 3));
        record.setHeightCm(parseInt(args, 4));
        record.setGenderCode(parseInt(args, 5));
        record.setAge(parseInt(args, 6));
        record.setWeightKg(parseInt(args, 7));
        repository.save(record);
    }

    private String[] split(String content) {
        return content.split(",");
    }

    private Integer parseInt(String[] args, int index) {
        if (index >= args.length || args[index].isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(args[index].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String[] args, int index) {
        if (index >= args.length || args[index].isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(args[index].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
