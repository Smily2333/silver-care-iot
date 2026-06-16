package com.silvercare.iot.api;

import com.silvercare.iot.api.dto.MiniappOverviewResponse;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Size;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/miniapp/devices")
public class MiniappDeviceController {

    private final DeviceRepository deviceRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final LocationRecordRepository locationRecordRepository;

    public MiniappDeviceController(DeviceRepository deviceRepository,
                                   HealthRecordRepository healthRecordRepository,
                                   LocationRecordRepository locationRecordRepository) {
        this.deviceRepository = deviceRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.locationRecordRepository = locationRecordRepository;
    }

    @GetMapping("/{deviceNo}/overview")
    public MiniappOverviewResponse overview(@PathVariable String deviceNo) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        HealthRecord latestHealth = healthRecordRepository
                .findFirstByDeviceIdOrderByMeasuredAtDesc(device.getId()).orElse(null);
        LocationRecord latestLocation = locationRecordRepository
                .findFirstByDeviceIdOrderByLocatedAtDesc(device.getId()).orElse(null);
        return MiniappOverviewResponse.of(device, latestHealth, latestLocation);
    }

    @GetMapping("/{deviceNo}/health-records")
    public List<HealthRecord> healthRecords(@PathVariable String deviceNo,
                                            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(size, 100);
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return healthRecordRepository.findTop100ByDeviceIdOrderByMeasuredAtDesc(device.getId())
                .stream().limit(clampedSize).toList();
    }

    @GetMapping("/{deviceNo}/location-records")
    public List<LocationRecord> locationRecords(@PathVariable String deviceNo,
                                                @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(size, 100);
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return locationRecordRepository.findByDeviceIdAndGpsValidTrueOrderByLocatedAtDesc(
                device.getId(), PageRequest.of(0, clampedSize));
    }

    public record BindRequest(
            @NotBlank String deviceNo,
            @NotBlank String ownerName
    ) {}

    @PostMapping("/bind")
    public Device bind(@Valid @RequestBody BindRequest req) {
        Device device = deviceRepository.findByDeviceNo(req.deviceNo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        device.setOwnerName(req.ownerName());
        return deviceRepository.save(device);
    }

    public record UpdateOwnerNameRequest(
            @NotBlank @Size(max = 64) String ownerName
    ) {}

    @PatchMapping("/{deviceNo}/owner-name")
    public Device updateOwnerName(@PathVariable String deviceNo,
                                  @Valid @RequestBody UpdateOwnerNameRequest req) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        device.setOwnerName(req.ownerName());
        return deviceRepository.save(device);
    }
}
