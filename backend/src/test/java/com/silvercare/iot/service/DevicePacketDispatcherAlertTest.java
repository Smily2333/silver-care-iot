package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.domain.entity.RawPacketLog;
import com.silvercare.iot.tcp.DeviceConnection;
import com.silvercare.iot.tcp.DeviceConnectionRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DevicePacketDispatcherAlertTest {

    private final DeviceConnectionRegistry registry = mock(DeviceConnectionRegistry.class);
    private final DeviceService deviceService = mock(DeviceService.class);
    private final RawPacketLogService rawPacketLogService = mock(RawPacketLogService.class);
    private final HealthDataService healthDataService = mock(HealthDataService.class);
    private final LocationDataService locationDataService = mock(LocationDataService.class);
    private final FallAlertService fallAlertService = mock(FallAlertService.class);
    private final DeviceConnection connection = mock(DeviceConnection.class);

    private DevicePacketDispatcher dispatcher() {
        return new DevicePacketDispatcher(registry, deviceService, rawPacketLogService,
                healthDataService, locationDataService, fallAlertService);
    }

    @Test
    void alPacket_savesLocationAndAlert_andSendsAck() throws Exception {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        RawPacketLog packetLog = new RawPacketLog();
        var idField = RawPacketLog.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(packetLog, 7L);

        LocationRecord locationRecord = new LocationRecord();

        when(deviceService.ensureOnline(any())).thenReturn(device);
        when(rawPacketLogService.saveSuccess(any())).thenReturn(packetLog);
        when(locationDataService.saveLocation(any(), any(), any())).thenReturn(locationRecord);

        dispatcher().dispatch(
                "[3G*2016001000*0055*AL,120118,070625,A,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010]",
                connection
        );

        verify(locationDataService).saveLocation(eq(device), any(), eq(7L));
        verify(fallAlertService).saveAlert(eq(device), any(), eq(locationRecord), eq(7L));
        verify(connection).send(contains("AL"));
    }
}
