package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.repository.FallAlertRepository;
import com.silvercare.iot.security.MiniappPrincipal;
import com.silvercare.iot.service.DeviceAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiniappAlertControllerTest {

    private static final MiniappPrincipal PRINCIPAL = new MiniappPrincipal(10L);
    private final DeviceAccessService deviceAccessService = mock(DeviceAccessService.class);
    private final FallAlertRepository fallAlertRepository = mock(FallAlertRepository.class);

    private MiniappAlertController controller() {
        return new MiniappAlertController(deviceAccessService, fallAlertRepository);
    }

    @Test
    void listReturnsAlertsForBoundDevice() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        FallAlert alert = new FallAlert();
        alert.setDeviceId(1L);
        alert.setAlertedAt(Instant.now());
        when(deviceAccessService.requireBoundDevice(10L, "DEV001")).thenReturn(device);
        when(fallAlertRepository.findTop20ByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(List.of(alert));

        assertThat(controller().list(PRINCIPAL, "DEV001", 20)).hasSize(1);
    }

    @Test
    void listClampsInvalidSizeToAtLeastOne() {
        Device device = new Device();
        when(deviceAccessService.requireBoundDevice(10L, "DEV001")).thenReturn(device);
        FallAlert first = new FallAlert();
        first.setAlertedAt(Instant.now());
        FallAlert second = new FallAlert();
        second.setAlertedAt(Instant.now());
        when(fallAlertRepository.findTop20ByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(List.of(first, second));

        assertThat(controller().list(PRINCIPAL, "DEV001", -5)).hasSize(1);
    }

    @Test
    void listRejectsUnavailableDevice() {
        when(deviceAccessService.requireBoundDevice(10L, "NOTEXIST"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        assertThatThrownBy(() -> controller().list(PRINCIPAL, "NOTEXIST", 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void latestReturnsNoContentWithoutAlerts() {
        Device device = new Device();
        when(deviceAccessService.requireBoundDevice(10L, "DEV001")).thenReturn(device);
        when(fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(Optional.empty());

        assertThat(controller().latest(PRINCIPAL, "DEV001").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void latestReturnsAlert() {
        Device device = new Device();
        FallAlert alert = new FallAlert();
        alert.setAlertedAt(Instant.now());
        when(deviceAccessService.requireBoundDevice(10L, "DEV001")).thenReturn(device);
        when(fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(Optional.of(alert));

        var response = controller().latest(PRINCIPAL, "DEV001");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(alert);
    }
}
