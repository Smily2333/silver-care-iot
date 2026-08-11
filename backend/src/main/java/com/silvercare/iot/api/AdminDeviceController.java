package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.api.dto.AdminLocationRecordResponse;
import com.silvercare.iot.api.dto.AdminHealthSummaryResponse;
import com.silvercare.iot.service.HealthSummaryService;
import com.silvercare.iot.protocol.ProtocolParser;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import com.silvercare.iot.tcp.DeviceConnection;
import com.silvercare.iot.tcp.DeviceConnectionRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/devices")
public class AdminDeviceController {

    private final DeviceRepository deviceRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final LocationRecordRepository locationRecordRepository;
    private final DeviceConnectionRegistry connectionRegistry;
    private final HealthSummaryService healthSummaryService;
    private final ProtocolParser parser = new ProtocolParser();

    public AdminDeviceController(DeviceRepository deviceRepository,
                                 HealthRecordRepository healthRecordRepository,
                                 LocationRecordRepository locationRecordRepository,
                                 DeviceConnectionRegistry connectionRegistry,
                                 HealthSummaryService healthSummaryService) {
        this.deviceRepository = deviceRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.locationRecordRepository = locationRecordRepository;
        this.connectionRegistry = connectionRegistry;
        this.healthSummaryService = healthSummaryService;
    }

    @GetMapping
    public Page<Device> list(Pageable pageable) {
        return deviceRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public Device get(@PathVariable Long id) {
        return deviceRepository.findById(id).orElseThrow();
    }

    @GetMapping("/{id}/latest-health")
    public HealthRecord latestHealth(@PathVariable Long id) {
        return healthRecordRepository.findFirstByDeviceIdOrderByMeasuredAtDesc(id).orElse(null);
    }

    @GetMapping("/{id}/health-records")
    public List<HealthRecord> healthRecords(@PathVariable Long id) {
        return healthRecordRepository.findTop100ByDeviceIdOrderByMeasuredAtDesc(id);
    }

    @GetMapping("/{id}/health-summary")
    public AdminHealthSummaryResponse healthSummary(@PathVariable Long id) {
        deviceRepository.findById(id).orElseThrow();
        return healthSummaryService.get(id);
    }

    @GetMapping("/{id}/latest-location")
    public AdminLocationRecordResponse latestLocation(@PathVariable Long id) {
        return locationRecordRepository.findFirstByDeviceIdAndLocatedAtBeforeOrderByLocatedAtDesc(id, locationCutoff())
                .map(AdminLocationRecordResponse::from)
                .orElse(null);
    }

    @GetMapping("/{id}/location-records")
    public List<AdminLocationRecordResponse> locationRecords(@PathVariable Long id) {
        return locationRecordRepository.findTop100ByDeviceIdAndLocatedAtBeforeOrderByLocatedAtDesc(
                        id, locationCutoff()).stream()
                .map(AdminLocationRecordResponse::from)
                .toList();
    }

    private Instant locationCutoff() {
        return Instant.now().plus(Duration.ofDays(30));
    }

    @PostMapping("/{id}/send-command")
    public Map<String, String> sendCommand(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Device device = deviceRepository.findById(id).orElseThrow();
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required");
        }
        String vendor = body.getOrDefault("vendor", "3G");
        String packet = parser.build(vendor, device.getDeviceNo(), content);
        DeviceConnection connection = connectionRegistry.find(device.getDeviceNo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Device not connected"));
        try {
            connection.send(packet);
            return Map.of("status", "sent", "packet", packet);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Send failed: " + e.getMessage());
        }
    }

    public record PatchRequest(String ownerName) {}

    @PatchMapping("/{id}")
    public Device patch(@PathVariable Long id, @RequestBody PatchRequest req) {
        if (req.ownerName() == null || req.ownerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerName must not be blank");
        }
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        device.setOwnerName(req.ownerName());
        return deviceRepository.save(device);
    }
}
