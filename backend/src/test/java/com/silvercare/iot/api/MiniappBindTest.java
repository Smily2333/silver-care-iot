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

class MiniappBindTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private MiniappDeviceController controller() {
        return new MiniappDeviceController(deviceRepository, null, null);
    }

    @Test
    void bind_setsOwnerNameAndReturnsDevice() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller().bind(new MiniappDeviceController.BindRequest("DEV001", "张奶奶"));

        assertThat(result.getOwnerName()).isEqualTo("张奶奶");
        verify(deviceRepository).save(device);
    }

    @Test
    void bind_deviceNotFound_throws404() {
        when(deviceRepository.findByDeviceNo("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                controller().bind(new MiniappDeviceController.BindRequest("NOTEXIST", "张奶奶")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void bind_overwritesExistingOwnerName() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        device.setOwnerName("旧姓名");
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller().bind(new MiniappDeviceController.BindRequest("DEV001", "新姓名"));

        assertThat(result.getOwnerName()).isEqualTo("新姓名");
    }
}
