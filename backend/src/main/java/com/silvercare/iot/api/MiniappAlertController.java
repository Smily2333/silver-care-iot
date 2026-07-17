package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.repository.FallAlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.silvercare.iot.security.MiniappPrincipal;
import com.silvercare.iot.service.DeviceAccessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/miniapp/devices")
public class MiniappAlertController {

    private final DeviceAccessService deviceAccessService;
    private final FallAlertRepository fallAlertRepository;

    public MiniappAlertController(DeviceAccessService deviceAccessService,
                                  FallAlertRepository fallAlertRepository) {
        this.deviceAccessService = deviceAccessService;
        this.fallAlertRepository = fallAlertRepository;
    }

    @GetMapping("/{deviceNo}/fall-alerts")
    public List<FallAlert> list(@AuthenticationPrincipal MiniappPrincipal principal,
                                @PathVariable String deviceNo,
                                @RequestParam(defaultValue = "20") int size) {
        Device device = deviceAccessService.requireBoundDevice(principal.userId(), deviceNo);
        int clamped = Math.max(1, Math.min(size, 20));
        return fallAlertRepository.findTop20ByDeviceIdOrderByAlertedAtDesc(device.getId())
                .stream().limit(clamped).toList();
    }

    @GetMapping("/{deviceNo}/fall-alerts/latest")
    public ResponseEntity<FallAlert> latest(@AuthenticationPrincipal MiniappPrincipal principal,
                                            @PathVariable String deviceNo) {
        Device device = deviceAccessService.requireBoundDevice(principal.userId(), deviceNo);
        return fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
