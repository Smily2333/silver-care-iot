package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.security.MiniappPrincipal;
import com.silvercare.iot.service.DeviceAccessService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniappBindTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final DeviceAccessService deviceAccessService = mock(DeviceAccessService.class);
    private final MiniappDeviceController controller =
            new MiniappDeviceController(deviceRepository, null, null, deviceAccessService);

    @Test
    void bindUsesAuthenticatedUser() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        device.setOwnerName("张奶奶");
        when(deviceAccessService.bind(10L, "DEV001", "张奶奶")).thenReturn(device);

        Device result = controller.bind(
                new MiniappPrincipal(10L),
                new MiniappDeviceController.BindRequest("DEV001", "张奶奶"));

        assertThat(result.getOwnerName()).isEqualTo("张奶奶");
        verify(deviceAccessService).bind(10L, "DEV001", "张奶奶");
    }
}
