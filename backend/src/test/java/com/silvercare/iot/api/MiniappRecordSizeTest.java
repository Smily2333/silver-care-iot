package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import com.silvercare.iot.security.MiniappPrincipal;
import com.silvercare.iot.service.DeviceAccessService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniappRecordSizeTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final HealthRecordRepository healthRecordRepository = mock(HealthRecordRepository.class);
    private final LocationRecordRepository locationRecordRepository = mock(LocationRecordRepository.class);
    private final DeviceAccessService deviceAccessService = mock(DeviceAccessService.class);
    private final MiniappPrincipal principal = new MiniappPrincipal(10L);
    private final MiniappDeviceController controller = new MiniappDeviceController(
            deviceRepository, healthRecordRepository, locationRecordRepository, deviceAccessService);

    @Test
    void healthRecordsClampsNegativeSizeToOne() {
        Device device = device(1L);
        when(deviceAccessService.requireBoundDevice(10L, "DEV001")).thenReturn(device);
        when(healthRecordRepository.findTop100ByDeviceIdOrderByMeasuredAtDesc(1L))
                .thenReturn(List.of(new HealthRecord(), new HealthRecord()));

        assertThat(controller.healthRecords(principal, "DEV001", -1)).hasSize(1);
    }

    @Test
    void locationRecordsClampsNegativeSizeToOne() {
        Device device = device(1L);
        when(deviceAccessService.requireBoundDevice(10L, "DEV001")).thenReturn(device);

        controller.locationRecords(principal, "DEV001", -1);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(locationRecordRepository)
                .findByDeviceIdAndGpsValidTrueOrderByLocatedAtDesc(eq(1L), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
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
