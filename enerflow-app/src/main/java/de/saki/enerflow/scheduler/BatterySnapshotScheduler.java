// scheduler/BatterySnapshotScheduler.java
package de.saki.enerflow.scheduler;

import de.saki.enerflow.adapter.battery.sonnen.SonnenBatterySnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically captures a sonnenBatterie status snapshot and persists it.
 * Runs on the same interval as EnergyManagerScheduler, but is a separate
 * component — persisting data and making automation decisions are two
 * distinct responsibilities, even though they currently share a cadence.
 *
 * @author saki
 */
@Component
@Profile("!test")
public class BatterySnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatterySnapshotScheduler.class);

    private final SonnenBatterySnapshotService snapshotService;

    public BatterySnapshotScheduler(SonnenBatterySnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Scheduled(fixedRateString = "${enerflow.evaluation-interval-ms}")
    public void poll() {
        try {
            snapshotService.captureAndSave();
        } catch (Exception e) {
            // Catch-all so a temporary battery connection issue doesn't stop
            // the scheduler permanently (same reasoning as EnergyManagerScheduler).
            log.error("Failed to capture battery snapshot: {}", e.getMessage(), e);
        }
    }
}