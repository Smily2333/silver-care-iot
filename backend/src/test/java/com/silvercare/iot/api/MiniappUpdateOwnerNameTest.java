package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MiniappUpdateOwnerNameTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private MiniappDeviceController controller() {
        return new MiniappDeviceController(deviceRepository, null, null);
    }

    @Test
    void updateOwnerName_savesNewName() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        device.setOwnerName("旧名字");
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller().updateOwnerName(
                "DEV001",
                new MiniappDeviceController.UpdateOwnerNameRequest("新名字"));

        assertThat(result.getOwnerName()).isEqualTo("新名字");
        verify(deviceRepository).save(device);
    }

    @Test
    void updateOwnerName_deviceNotFound_throws404() {
        when(deviceRepository.findByDeviceNo("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                controller().updateOwnerName(
                        "NOTEXIST",
                        new MiniappDeviceController.UpdateOwnerNameRequest("张奶奶")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
