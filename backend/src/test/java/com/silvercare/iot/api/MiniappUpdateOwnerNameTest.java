package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.security.MiniappPrincipal;
import com.silvercare.iot.service.DeviceAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniappUpdateOwnerNameTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final DeviceAccessService deviceAccessService = mock(DeviceAccessService.class);
    private final MiniappDeviceController controller =
            new MiniappDeviceController(deviceRepository, null, null, deviceAccessService);
    private final MiniappPrincipal principal = new MiniappPrincipal(10L);

    @Test
    void updateOwnerNameSavesBoundDevice() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        device.setOwnerName("旧名字");
        when(deviceAccessService.requireBoundDevice(10L, "DEV001")).thenReturn(device);
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller.updateOwnerName(
                principal, "DEV001", new MiniappDeviceController.UpdateOwnerNameRequest("新名字"));

        assertThat(result.getOwnerName()).isEqualTo("新名字");
        verify(deviceRepository).save(device);
    }

    @Test
    void updateOwnerNameRejectsUnboundDevice() {
        when(deviceAccessService.requireBoundDevice(10L, "OTHER"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Device is not bound"));

        assertThatThrownBy(() -> controller.updateOwnerName(
                principal, "OTHER", new MiniappDeviceController.UpdateOwnerNameRequest("张奶奶")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
