package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.DeviceBinding;
import com.silvercare.iot.domain.entity.MiniappUser;
import com.silvercare.iot.repository.DeviceBindingRepository;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.MiniappUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceAccessServiceTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final DeviceBindingRepository bindingRepository = mock(DeviceBindingRepository.class);
    private final MiniappUserRepository miniappUserRepository = mock(MiniappUserRepository.class);
    private final DeviceAccessService service =
            new DeviceAccessService(deviceRepository, bindingRepository, miniappUserRepository);

    @Test
    void requireBoundDeviceAllowsOwner() {
        Device device = device(5L);
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(bindingRepository.existsByUserIdAndDeviceId(10L, 5L)).thenReturn(true);

        assertThat(service.requireBoundDevice(10L, "DEV001")).isSameAs(device);
    }

    @Test
    void requireBoundDeviceRejectsOtherUser() {
        Device device = device(5L);
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.requireBoundDevice(11L, "DEV001"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void bindClaimsUnboundDevice() {
        Device device = device(5L);
        when(miniappUserRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mock(MiniappUser.class)));
        when(deviceRepository.findByDeviceNoForUpdate("DEV001")).thenReturn(Optional.of(device));
        when(bindingRepository.findByUserIdAndDeviceId(10L, 5L)).thenReturn(Optional.empty());
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = service.bind(10L, "DEV001", "张奶奶");

        ArgumentCaptor<DeviceBinding> binding = ArgumentCaptor.forClass(DeviceBinding.class);
        verify(bindingRepository).save(binding.capture());
        assertThat(binding.getValue().getUserId()).isEqualTo(10L);
        assertThat(binding.getValue().getDeviceId()).isEqualTo(5L);
        assertThat(result.getOwnerName()).isEqualTo("张奶奶");
    }

    @Test
    void bindAllowsASecondUserOnTheSameDevice() {
        Device device = device(5L);
        when(miniappUserRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mock(MiniappUser.class)));
        when(deviceRepository.findByDeviceNoForUpdate("DEV001")).thenReturn(Optional.of(device));
        when(bindingRepository.findByUserIdAndDeviceId(10L, 5L)).thenReturn(Optional.empty());
        when(bindingRepository.countByDeviceId(5L)).thenReturn(1L);
        when(deviceRepository.save(device)).thenReturn(device);

        service.bind(10L, "DEV001", "张奶奶");

        verify(bindingRepository).save(org.mockito.ArgumentMatchers.any(DeviceBinding.class));
    }

    @Test
    void bindRejectsAFifthDeviceForTheSameUser() {
        Device device = device(5L);
        when(miniappUserRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mock(MiniappUser.class)));
        when(deviceRepository.findByDeviceNoForUpdate("DEV001")).thenReturn(Optional.of(device));
        when(bindingRepository.findByUserIdAndDeviceId(10L, 5L)).thenReturn(Optional.empty());
        when(bindingRepository.countByUserId(10L)).thenReturn(4L);

        assertThatThrownBy(() -> service.bind(10L, "DEV001", "张奶奶"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(bindingRepository, never()).save(org.mockito.ArgumentMatchers.any(DeviceBinding.class));
    }

    @Test
    void bindRejectsAFifthUserForTheSameDevice() {
        Device device = device(5L);
        when(miniappUserRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mock(MiniappUser.class)));
        when(deviceRepository.findByDeviceNoForUpdate("DEV001")).thenReturn(Optional.of(device));
        when(bindingRepository.findByUserIdAndDeviceId(10L, 5L)).thenReturn(Optional.empty());
        when(bindingRepository.countByDeviceId(5L)).thenReturn(4L);

        assertThatThrownBy(() -> service.bind(10L, "DEV001", "张奶奶"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        verify(bindingRepository, never()).save(org.mockito.ArgumentMatchers.any(DeviceBinding.class));
    }

    @Test
    void rebindingTheSamePairRemainsIdempotentAtTheLimits() {
        Device device = device(5L);
        DeviceBinding existing = new DeviceBinding();
        existing.setUserId(10L);
        existing.setDeviceId(5L);
        when(miniappUserRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mock(MiniappUser.class)));
        when(deviceRepository.findByDeviceNoForUpdate("DEV001")).thenReturn(Optional.of(device));
        when(bindingRepository.findByUserIdAndDeviceId(10L, 5L)).thenReturn(Optional.of(existing));
        when(bindingRepository.countByUserId(10L)).thenReturn(4L);
        when(bindingRepository.countByDeviceId(5L)).thenReturn(4L);
        when(deviceRepository.save(device)).thenReturn(device);

        service.bind(10L, "DEV001", "新称呼");

        verify(bindingRepository, never()).save(org.mockito.ArgumentMatchers.any(DeviceBinding.class));
        assertThat(device.getOwnerName()).isEqualTo("新称呼");
    }

    private Device device(Long id) {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        try {
            var field = Device.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(device, id);
            return device;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
