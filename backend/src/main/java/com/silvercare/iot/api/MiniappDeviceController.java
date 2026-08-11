package com.silvercare.iot.api;

import com.silvercare.iot.api.dto.MiniappOverviewResponse;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import com.silvercare.iot.security.MiniappPrincipal;
import com.silvercare.iot.service.DeviceAccessService;
import com.silvercare.iot.service.HealthSummaryService;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/miniapp/devices")
public class MiniappDeviceController {

    private final DeviceRepository deviceRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final LocationRecordRepository locationRecordRepository;
    private final DeviceAccessService deviceAccessService;
    private final HealthSummaryService healthSummaryService;

    public MiniappDeviceController(DeviceRepository deviceRepository,
                                   HealthRecordRepository healthRecordRepository,
                                   LocationRecordRepository locationRecordRepository,
                                   DeviceAccessService deviceAccessService,
                                   HealthSummaryService healthSummaryService) {
        this.deviceRepository = deviceRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.locationRecordRepository = locationRecordRepository;
        this.deviceAccessService = deviceAccessService;
        this.healthSummaryService = healthSummaryService;
    }

    @GetMapping("/{deviceNo}/overview")
    public MiniappOverviewResponse overview(@AuthenticationPrincipal MiniappPrincipal principal,
                                            @PathVariable String deviceNo) {
        Device device = deviceAccessService.requireBoundDevice(principal.userId(), deviceNo);
        HealthRecord latestHealth = healthRecordRepository
                .findFirstByDeviceIdOrderByMeasuredAtDesc(device.getId()).orElse(null);
        LocationRecord latestLocation = locationRecordRepository
                .findFirstByDeviceIdAndLocatedAtBeforeOrderByLocatedAtDesc(
                        device.getId(), locationCutoff()).orElse(null);
        return MiniappOverviewResponse.of(device, latestHealth, latestLocation,
                healthSummaryService == null ? null : healthSummaryService.get(device.getId()));
    }

    @GetMapping("/{deviceNo}/health-records")
    public List<HealthRecord> healthRecords(@AuthenticationPrincipal MiniappPrincipal principal,
                                            @PathVariable String deviceNo,
                                            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.max(1, Math.min(size, 100));
        Device device = deviceAccessService.requireBoundDevice(principal.userId(), deviceNo);
        return healthRecordRepository.findTop100ByDeviceIdOrderByMeasuredAtDesc(device.getId())
                .stream().limit(clampedSize).toList();
    }

    @GetMapping("/{deviceNo}/location-records")
    public List<LocationRecord> locationRecords(@AuthenticationPrincipal MiniappPrincipal principal,
                                                @PathVariable String deviceNo,
                                                 @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.max(1, Math.min(size, 100));
        Device device = deviceAccessService.requireBoundDevice(principal.userId(), deviceNo);
        return locationRecordRepository.findByDeviceIdAndGpsValidTrueAndLocatedAtBeforeOrderByLocatedAtDesc(
                device.getId(), locationCutoff(), PageRequest.of(0, clampedSize));
    }

    private Instant locationCutoff() {
        return Instant.now().plus(Duration.ofDays(30));
    }

    public record BindRequest(
            @NotBlank @Size(max = 64) String deviceNo,
            @NotBlank @Size(max = 64) String ownerName
    ) {}

    @PostMapping("/bind")
    public Device bind(@AuthenticationPrincipal MiniappPrincipal principal,
                       @Valid @RequestBody BindRequest req) {
        return deviceAccessService.bind(principal.userId(), req.deviceNo(), req.ownerName());
    }

    public record UpdateOwnerNameRequest(
            @NotBlank @Size(max = 64) String ownerName
    ) {}

    @PatchMapping("/{deviceNo}/owner-name")
    public Device updateOwnerName(@AuthenticationPrincipal MiniappPrincipal principal,
                                  @PathVariable String deviceNo,
                                  @Valid @RequestBody UpdateOwnerNameRequest req) {
        Device device = deviceAccessService.requireBoundDevice(principal.userId(), deviceNo);
        device.setOwnerName(req.ownerName());
        return deviceRepository.save(device);
    }
}
