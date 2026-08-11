package com.silvercare.iot.service;

import com.silvercare.iot.domain.HealthMeasurementStatus;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.HealthRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class HealthDataService {

    private static final BigDecimal MIN_TEMPERATURE = new BigDecimal("30.0");
    private static final BigDecimal MAX_TEMPERATURE = new BigDecimal("45.0");

    private final HealthRecordRepository repository;
    private final DeviceActionService actionService;

    public HealthDataService(HealthRecordRepository repository, DeviceActionService actionService) {
        this.repository = repository;
        this.actionService = actionService;
    }

    public HealthRecord saveTemperature(Device device, ProtocolFrame frame, Long rawPacketId) {
        String[] args = split(frame.content());
        HealthRecord record = baseRecord(device, frame, rawPacketId);
        record.setTemperatureType(parseInt(args, 1));
        BigDecimal temperature = parseDecimal(args, 2);
        HealthMeasurementStatus status = temperatureStatus(temperature);
        record.setTemperatureStatus(status);
        if (status == HealthMeasurementStatus.INVALID) {
            record.setInvalidReason("体温测量值缺失或为设备失败状态值");
        } else {
            record.setBodyTemperature(temperature);
        }
        HealthRecord saved = repository.save(record);
        actionService.complete(device.getId(), com.silvercare.iot.domain.DeviceActionType.MEASURE_TEMPERATURE,
                "HEALTH", saved.getId(), status != HealthMeasurementStatus.INVALID);
        return saved;
    }

    public HealthRecord saveBloodPressureHeartRate(Device device, ProtocolFrame frame, Long rawPacketId) {
        String[] args = split(frame.content());
        HealthRecord record = baseRecord(device, frame, rawPacketId);
        Integer systolic = parseInt(args, 1);
        Integer diastolic = parseInt(args, 2);
        Integer heartRate = parseInt(args, 3);
        HealthMeasurementStatus heartStatus = heartRateStatus(heartRate);
        HealthMeasurementStatus pressureStatus = bloodPressureStatus(systolic, diastolic);
        record.setHeartRateStatus(heartStatus);
        record.setBloodPressureStatus(pressureStatus);
        if (heartStatus != HealthMeasurementStatus.INVALID) {
            record.setHeartRate(heartRate);
        }
        if (pressureStatus != HealthMeasurementStatus.INVALID) {
            record.setSystolicPressure(systolic);
            record.setDiastolicPressure(diastolic);
        }
        record.setHeightCm(parseInt(args, 4));
        record.setGenderCode(parseInt(args, 5));
        record.setAge(parseInt(args, 6));
        record.setWeightKg(parseInt(args, 7));
        record.setInvalidReason(invalidReason(heartStatus, pressureStatus));
        HealthRecord saved = repository.save(record);
        actionService.complete(device.getId(), com.silvercare.iot.domain.DeviceActionType.MEASURE_HEART_RATE,
                "HEALTH", saved.getId(), heartStatus != HealthMeasurementStatus.INVALID);
        return saved;
    }

    private HealthRecord baseRecord(Device device, ProtocolFrame frame, Long rawPacketId) {
        HealthRecord record = new HealthRecord();
        record.setDeviceId(device.getId());
        record.setSourceCommand(frame.command());
        record.setRawPacketId(rawPacketId);
        return record;
    }

    private HealthMeasurementStatus temperatureStatus(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ONE) <= 0) return HealthMeasurementStatus.INVALID;
        if (value.compareTo(MIN_TEMPERATURE) < 0) return HealthMeasurementStatus.TOO_LOW;
        if (value.compareTo(MAX_TEMPERATURE) > 0) return HealthMeasurementStatus.TOO_HIGH;
        return HealthMeasurementStatus.VALID;
    }

    private HealthMeasurementStatus heartRateStatus(Integer value) {
        if (value == null || value <= 0) return HealthMeasurementStatus.INVALID;
        if (value < 30) return HealthMeasurementStatus.TOO_LOW;
        if (value > 220) return HealthMeasurementStatus.TOO_HIGH;
        return HealthMeasurementStatus.VALID;
    }

    private HealthMeasurementStatus bloodPressureStatus(Integer systolic, Integer diastolic) {
        if (systolic == null || diastolic == null || systolic <= 0 || diastolic <= 0) {
            return HealthMeasurementStatus.INVALID;
        }
        if (systolic < 60 || diastolic < 30) return HealthMeasurementStatus.TOO_LOW;
        if (systolic > 260 || diastolic > 180) return HealthMeasurementStatus.TOO_HIGH;
        return HealthMeasurementStatus.VALID;
    }

    private String invalidReason(HealthMeasurementStatus heart, HealthMeasurementStatus pressure) {
        List<String> reasons = new ArrayList<>();
        if (heart == HealthMeasurementStatus.INVALID) reasons.add("心率值缺失或为 0");
        if (pressure == HealthMeasurementStatus.INVALID) reasons.add("血压值缺失或为 0");
        return reasons.isEmpty() ? null : String.join("；", reasons);
    }

    private String[] split(String content) {
        return content.split(",");
    }

    private Integer parseInt(String[] args, int index) {
        if (index >= args.length || args[index].isBlank()) return null;
        try {
            return Integer.valueOf(args[index].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String[] args, int index) {
        if (index >= args.length || args[index].isBlank()) return null;
        try {
            return new BigDecimal(args[index].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
