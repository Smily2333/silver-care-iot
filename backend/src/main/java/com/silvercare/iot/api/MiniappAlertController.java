package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.FallAlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/miniapp/devices")
public class MiniappAlertController {

    private final DeviceRepository deviceRepository;
    private final FallAlertRepository fallAlertRepository;

    public MiniappAlertController(DeviceRepository deviceRepository,
                                  FallAlertRepository fallAlertRepository) {
        this.deviceRepository = deviceRepository;
        this.fallAlertRepository = fallAlertRepository;
    }

    @GetMapping("/{deviceNo}/fall-alerts")
    public List<FallAlert> list(@PathVariable String deviceNo,
                                @RequestParam(defaultValue = "20") int size) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        int clamped = Math.max(1, Math.min(size, 20));
        return fallAlertRepository.findTop20ByDeviceIdOrderByAlertedAtDesc(device.getId())
                .stream().limit(clamped).toList();
    }

    @GetMapping("/{deviceNo}/fall-alerts/latest")
    public ResponseEntity<FallAlert> latest(@PathVariable String deviceNo) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
