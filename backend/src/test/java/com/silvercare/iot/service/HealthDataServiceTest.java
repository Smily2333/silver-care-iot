package com.silvercare.iot.service;

import com.silvercare.iot.domain.HealthMeasurementStatus;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.HealthRecordRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthDataServiceTest {
    private final HealthRecordRepository repository = mock(HealthRecordRepository.class);
    private final DeviceActionService actionService = mock(DeviceActionService.class);
    private final HealthDataService service = new HealthDataService(repository, actionService);

    @Test
    void temperatureFailureValueIsNotExposedAsMeasurement() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        HealthRecord record = service.saveTemperature(new Device(), frame("btemp2,0,1"), 1L);
        assertThat(record.getBodyTemperature()).isNull();
        assertThat(record.getTemperatureStatus()).isEqualTo(HealthMeasurementStatus.INVALID);
    }

    @Test
    void zeroBloodPressureIsInvalidWhileHeartRateCanRemainValid() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        HealthRecord record = service.saveBloodPressureHeartRate(new Device(), frame("bphrt,0,0,78"), 2L);
        assertThat(record.getHeartRate()).isEqualTo(78);
        assertThat(record.getHeartRateStatus()).isEqualTo(HealthMeasurementStatus.VALID);
        assertThat(record.getSystolicPressure()).isNull();
        assertThat(record.getBloodPressureStatus()).isEqualTo(HealthMeasurementStatus.INVALID);
    }

    @Test
    void oxygenUsesTheSecondMeasurementField() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        HealthRecord record = service.saveOxygen(new Device(), frame("oxygen,0,99"), 3L);
        assertThat(record.getOxygenSaturation()).isEqualTo(99);
        assertThat(record.getOxygenStatus()).isEqualTo(HealthMeasurementStatus.VALID);
    }

    @Test
    void zeroOxygenIsStoredAsInvalidWithoutExposingAValue() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        HealthRecord record = service.saveOxygen(new Device(), frame("oxygen,0,0"), 4L);
        assertThat(record.getOxygenSaturation()).isNull();
        assertThat(record.getOxygenStatus()).isEqualTo(HealthMeasurementStatus.INVALID);
    }

    private ProtocolFrame frame(String content) {
        String command = content.substring(0, content.indexOf(','));
        return new ProtocolFrame("3G", "DEV001", "0000", content, command, "");
    }
}
