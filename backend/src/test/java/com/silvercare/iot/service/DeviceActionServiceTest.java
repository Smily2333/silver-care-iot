package com.silvercare.iot.service;

import com.silvercare.iot.config.DeviceActionProperties;
import com.silvercare.iot.config.AutomaticMonitoringProperties;
import com.silvercare.iot.domain.DeviceActionStatus;
import com.silvercare.iot.domain.DeviceActionType;
import com.silvercare.iot.domain.DeviceStatus;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.DeviceAction;
import com.silvercare.iot.repository.DeviceActionRepository;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.tcp.DeviceConnection;
import com.silvercare.iot.tcp.DeviceConnectionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceActionServiceTest {
    private final DeviceActionRepository repository = mock(DeviceActionRepository.class);
    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final DeviceConnectionRegistry registry = mock(DeviceConnectionRegistry.class);
    private final DeviceActionProperties properties = new DeviceActionProperties();
    private final AutomaticMonitoringProperties monitoringProperties = new AutomaticMonitoringProperties();
    private final DeviceCommandCatalog catalog = new DeviceCommandCatalog(properties, monitoringProperties);
    private final DeviceActionService service = new DeviceActionService(
            repository, deviceRepository, registry, catalog, properties);

    @Test
    void unconfirmedCommandIsRejectedBeforeSending() {
        Device device = device();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        assertThatThrownBy(() -> service.create(1L, DeviceActionType.LOCATE_NOW, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("通信指令待人工确认");
    }

    @Test
    void confirmedLocateCommandIsTrackedAsSent() throws Exception {
        Device device = device();
        DeviceConnection connection = mock(DeviceConnection.class);
        properties.getConfirmedTypes().add(DeviceActionType.LOCATE_NOW);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(registry.find("DEV001")).thenReturn(Optional.of(connection));
        when(connection.getLastSeenAt()).thenReturn(Instant.now());
        when(repository.findFirstByDeviceIdAndActionTypeAndStatusInOrderByRequestedAtDesc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(DeviceAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceAction action = service.create(1L, DeviceActionType.LOCATE_NOW, "admin");

        assertThat(action.getStatus()).isEqualTo(DeviceActionStatus.SENT);
        verify(connection).send("[3G*DEV001*0002*CR]");
    }

    @Test
    void healthActionRemainsBlockedUntilWearProtocolIsConfirmed() {
        Device device = device();
        DeviceConnection connection = mock(DeviceConnection.class);
        properties.getConfirmedTypes().add(DeviceActionType.MEASURE_HEART_RATE);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(registry.find("DEV001")).thenReturn(Optional.of(connection));
        when(connection.getLastSeenAt()).thenReturn(Instant.now());
        assertThat(service.capabilities(1L).stream()
                .filter(item -> item.type() == DeviceActionType.MEASURE_HEART_RATE).findFirst().orElseThrow().reason())
                .contains("佩戴状态位待确认");
    }

    @Test
    void locationIntervalConfigurationCompletesOnAcknowledgement() {
        Device device = device();
        DeviceAction action = new DeviceAction();
        action.setDeviceId(1L);
        action.setActionType(DeviceActionType.CONFIGURE_LOCATION_INTERVAL);
        action.setCommandName("UPLOAD");
        action.setCommandContent("UPLOAD,600");
        action.setStatus(DeviceActionStatus.SENT);
        when(repository.findFirstByDeviceIdAndCommandNameAndStatusInOrderByRequestedAtDesc(
                any(), any(), any())).thenReturn(Optional.of(action));

        service.acknowledge(device, "UPLOAD");

        assertThat(action.getStatus()).isEqualTo(DeviceActionStatus.COMPLETED);
        assertThat(action.getAcknowledgedAt()).isNotNull();
        assertThat(action.getCompletedAt()).isNotNull();
    }

    private Device device() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        device.setStatus(DeviceStatus.ONLINE);
        try {
            var field = Device.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(device, 1L);
            return device;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
