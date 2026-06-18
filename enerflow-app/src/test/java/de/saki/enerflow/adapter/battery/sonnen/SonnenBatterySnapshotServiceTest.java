// test/java/de/saki/enerflow/adapter/battery/sonnen/SonnenBatterySnapshotServiceTest.java
package de.saki.enerflow.adapter.battery.sonnen;

import de.saki.enerflow.core.domain.BatterySnapshot;
import de.saki.enerflow.core.repository.BatterySnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SonnenBatterySnapshotServiceTest {

    @Mock
    private SonnenBatteryAdapter sonnenBatteryAdapter;

    @Mock
    private BatterySnapshotRepository repository;

    @Test
    void captureAndSave_mapsAllFieldsAndComputesSurplus() {
        SonnenStatusResponse status = new SonnenStatusResponse(
                95, 2000, 500, -800, true, false);
        when(sonnenBatteryAdapter.fetchCurrentStatus()).thenReturn(status);

        SonnenBatterySnapshotService service =
                new SonnenBatterySnapshotService(sonnenBatteryAdapter, repository);

        service.captureAndSave();

        ArgumentCaptor<BatterySnapshot> captor = ArgumentCaptor.forClass(BatterySnapshot.class);
        verify(repository).save(captor.capture());

        BatterySnapshot saved = captor.getValue();
        assertThat(saved.getUsableSocPercent()).isEqualTo(95);
        assertThat(saved.getProductionWatts()).isEqualTo(2000);
        assertThat(saved.getConsumptionWatts()).isEqualTo(500);
        assertThat(saved.getBatteryPowerWatts()).isEqualTo(-800);
        assertThat(saved.isBatteryCharging()).isTrue();
        assertThat(saved.isBatteryDischarging()).isFalse();
        // 2000 - 500 - 800 (charging power) = 700
        assertThat(saved.getSurplusWatts()).isEqualTo(700);
    }
}