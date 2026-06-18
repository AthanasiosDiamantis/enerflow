// test/java/de/saki/enerflow/core/service/SnapshotRetentionServiceTest.java
package de.saki.enerflow.core.service;

import de.saki.enerflow.core.repository.BatterySnapshotRepository;
import de.saki.enerflow.core.repository.HeatpumpSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnapshotRetentionServiceTest {

    @Mock
    private HeatpumpSnapshotRepository heatpumpSnapshotRepository;

    @Mock
    private BatterySnapshotRepository batterySnapshotRepository;

    @Mock
    private DeviceConfigService deviceConfigService;

    @Test
    void purgeExpiredSnapshots_usesConfiguredRetentionThreshold() {
        when(deviceConfigService.getSnapshotRetentionDays()).thenReturn(730);

        SnapshotRetentionService service = new SnapshotRetentionService(
                heatpumpSnapshotRepository, batterySnapshotRepository, deviceConfigService);

        LocalDateTime before = LocalDateTime.now().minusDays(730);
        service.purgeExpiredSnapshots();
        LocalDateTime after = LocalDateTime.now().minusDays(730);

        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(heatpumpSnapshotRepository).deleteByTimestampBefore(thresholdCaptor.capture());
        verify(batterySnapshotRepository).deleteByTimestampBefore(thresholdCaptor.getValue());

        assertThat(thresholdCaptor.getValue()).isBetween(before, after);
    }
}