// adapter/battery/sonnen/SonnenBatterySnapshotService.java
package de.saki.enerflow.adapter.battery.sonnen;

import de.saki.enerflow.core.domain.BatterySnapshot;
import de.saki.enerflow.core.repository.BatterySnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Captures a single sonnenBatterie status reading and persists it as a
 * {@link BatterySnapshot}. Mirrors {@code HeatpumpSnapshotService} for the
 * heat pump, but simpler — sonnenBatterie returns all values in one REST
 * call, so there's no need to accumulate multiple content blocks.
 *
 * <p>Note: the surplus calculation here intentionally duplicates the one
 * line in {@link SonnenBatteryAdapter#getSurplusWatts()} rather than
 * calling it directly — calling it would trigger a second HTTP request
 * to the battery for the same status. If this formula changes, update
 * both places.
 *
 * @author saki
 */
@Service
public class SonnenBatterySnapshotService {

    private final SonnenBatteryAdapter sonnenBatteryAdapter;
    private final BatterySnapshotRepository repository;

    public SonnenBatterySnapshotService(SonnenBatteryAdapter sonnenBatteryAdapter,
                                        BatterySnapshotRepository repository) {
        this.sonnenBatteryAdapter = sonnenBatteryAdapter;
        this.repository = repository;
    }

    public void captureAndSave() {
        SonnenStatusResponse status = sonnenBatteryAdapter.fetchCurrentStatus();

        int batteryChargingWatts = Math.max(0, -status.batteryPowerWatts());
        int surplusWatts = status.productionWatts() - status.consumptionWatts() - batteryChargingWatts;

        BatterySnapshot snapshot = new BatterySnapshot();
        snapshot.setTimestamp(LocalDateTime.now());
        snapshot.setUsableSocPercent(status.usableStateOfChargePercent());
        snapshot.setProductionWatts(status.productionWatts());
        snapshot.setConsumptionWatts(status.consumptionWatts());
        snapshot.setBatteryPowerWatts(status.batteryPowerWatts());
        snapshot.setBatteryCharging(status.batteryCharging());
        snapshot.setBatteryDischarging(status.batteryDischarging());
        snapshot.setSurplusWatts(surplusWatts);

        repository.save(snapshot);
    }
}