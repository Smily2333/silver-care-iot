package com.silvercare.iot.api;

import com.silvercare.iot.api.dto.DeviceActionResponse;
import com.silvercare.iot.domain.DeviceActionType;
import com.silvercare.iot.service.DeviceActionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/devices/{deviceId}/actions")
public class AdminDeviceActionController {
    private final DeviceActionService service;

    public AdminDeviceActionController(DeviceActionService service) { this.service = service; }

    public record CreateRequest(@NotNull DeviceActionType type) {}

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DeviceActionResponse create(@PathVariable Long deviceId, @Valid @RequestBody CreateRequest request,
                                       Authentication authentication) {
        return DeviceActionResponse.from(service.create(deviceId, request.type(), authentication.getName()));
    }

    @GetMapping("/capabilities")
    public List<DeviceActionService.Capability> capabilities(@PathVariable Long deviceId) {
        return service.capabilities(deviceId);
    }

    @GetMapping("/{actionId}")
    public DeviceActionResponse get(@PathVariable Long deviceId, @PathVariable Long actionId) {
        return DeviceActionResponse.from(service.require(deviceId, actionId));
    }

    @GetMapping
    public List<DeviceActionResponse> list(@PathVariable Long deviceId) {
        return service.list(deviceId).stream().map(DeviceActionResponse::from).toList();
    }
}
