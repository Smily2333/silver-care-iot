package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/devices")
public class AdminDeviceController {

    private final DeviceRepository deviceRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final LocationRecordRepository locationRecordRepository;

    public AdminDeviceController(DeviceRepository deviceRepository,
                                 HealthRecordRepository healthRecordRepository,
                                 LocationRecordRepository locationRecordRepository) {
        this.deviceRepository = deviceRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.locationRecordRepository = locationRecordRepository;
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

    @GetMapping("/{id}/latest-location")
    public LocationRecord latestLocation(@PathVariable Long id) {
        return locationRecordRepository.findFirstByDeviceIdOrderByLocatedAtDesc(id).orElse(null);
    }

    @GetMapping("/{id}/location-records")
    public List<LocationRecord> locationRecords(@PathVariable Long id) {
        return locationRecordRepository.findTop100ByDeviceIdOrderByLocatedAtDesc(id);
    }
}
