package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.FallAlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MiniappAlertControllerTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final FallAlertRepository fallAlertRepository = mock(FallAlertRepository.class);

    private MiniappAlertController controller() {
        return new MiniappAlertController(deviceRepository, fallAlertRepository);
    }

    @Test
    void list_returnsAlertsForDevice() {
        Device device = new Device();
        device.setDeviceNo("DEV001");

        FallAlert alert = new FallAlert();
        alert.setDeviceId(1L);
        alert.setAlertedAt(Instant.now());

        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(fallAlertRepository.findTop20ByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(List.of(alert));

        List<FallAlert> result = controller().list("DEV001", 20);

        assertThat(result).hasSize(1);
    }

    @Test
    void list_clampsInvalidSizeToAtLeastOne() {
        Device device = new Device();
        device.setDeviceNo("DEV001");

        FallAlert first = new FallAlert();
        first.setAlertedAt(Instant.now());
        FallAlert second = new FallAlert();
        second.setAlertedAt(Instant.now());

        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(fallAlertRepository.findTop20ByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(List.of(first, second));

        List<FallAlert> result = controller().list("DEV001", -5);

        assertThat(result).hasSize(1);
    }

    @Test
    void list_deviceNotFound_throws404() {
        when(deviceRepository.findByDeviceNo("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller().list("NOTEXIST", 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void latest_noAlerts_returns204() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(Optional.empty());

        var response = controller().latest("DEV001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void latest_hasAlert_returns200() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        FallAlert alert = new FallAlert();
        alert.setAlertedAt(Instant.now());

        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(Optional.of(alert));

        var response = controller().latest("DEV001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(alert);
    }
}
