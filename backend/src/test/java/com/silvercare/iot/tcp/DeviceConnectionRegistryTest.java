package com.silvercare.iot.tcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceConnectionRegistryTest {

    @Test
    void replacingConnectionDoesNotLetOldConnectionRemoveCurrentOne() {
        DeviceConnectionRegistry registry = new DeviceConnectionRegistry();
        DeviceConnection oldConnection = mock(DeviceConnection.class);
        DeviceConnection newConnection = mock(DeviceConnection.class);
        when(oldConnection.getDeviceNo()).thenReturn("DEV001");
        when(newConnection.getDeviceNo()).thenReturn("DEV001");

        registry.register("DEV001", oldConnection);
        registry.register("DEV001", newConnection);

        verify(oldConnection).closeQuietly();
        assertThat(registry.remove(oldConnection)).isFalse();
        assertThat(registry.find("DEV001")).contains(newConnection);
        assertThat(registry.remove(newConnection)).isTrue();
        assertThat(registry.find("DEV001")).isEmpty();
    }
}
