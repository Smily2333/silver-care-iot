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

class AdminDevicePatchTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private AdminDeviceController controller() {
        return new AdminDeviceController(deviceRepository, null, null, null);
    }

    @Test
    void patch_setsOwnerName() {
        Device device = new Device();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller().patch(1L, new AdminDeviceController.PatchRequest("李爷爷"));

        assertThat(result.getOwnerName()).isEqualTo("李爷爷");
        verify(deviceRepository).save(device);
    }

    @Test
    void patch_deviceNotFound_throws404() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                controller().patch(99L, new AdminDeviceController.PatchRequest("李爷爷")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void patch_emptyOwnerName_throws400() {
        assertThatThrownBy(() ->
                controller().patch(1L, new AdminDeviceController.PatchRequest("  ")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
