// core/service/SnapshotRetentionService.java
package de.saki.enerflow.core.service;

import de.saki.enerflow.core.repository.BatterySnapshotRepository;
import de.saki.enerflow.core.repository.HeatpumpSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Deletes heatpump_snapshot and battery_snapshot rows older than the
 * configured retention period (default 730 days / ~2 years, per US-05-01).
 * Runs once at application startup (simple solution for AP3.2 — a recurring
 * @Scheduled cleanup could be added later if needed).
 *
 * <p>control_log is intentionally excluded — it's an audit trail and
 * should not be purged automatically.
 *
 * @author saki
 */
@Component
@Profile("!test")
public class SnapshotRetentionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRetentionService.class);

    private final HeatpumpSnapshotRepository heatpumpSnapshotRepository;
    private final BatterySnapshotRepository batterySnapshotRepository;
    private final DeviceConfigService deviceConfigService;

    public SnapshotRetentionService(HeatpumpSnapshotRepository heatpumpSnapshotRepository,
                                    BatterySnapshotRepository batterySnapshotRepository,
                                    DeviceConfigService deviceConfigService) {
        this.heatpumpSnapshotRepository = heatpumpSnapshotRepository;
        this.batterySnapshotRepository = batterySnapshotRepository;
        this.deviceConfigService = deviceConfigService;
    }

    @Override
    public void run(ApplicationArguments args) {
        purgeExpiredSnapshots();
    }

    public void purgeExpiredSnapshots() {
        int retentionDays = deviceConfigService.getSnapshotRetentionDays();
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);

        long deletedHeatpumpRows = heatpumpSnapshotRepository.deleteByTimestampBefore(threshold);
        long deletedBatteryRows = batterySnapshotRepository.deleteByTimestampBefore(threshold);

        log.info("Snapshot retention cleanup: removed {} heatpump rows and {} battery rows older than {} days",
                deletedHeatpumpRows, deletedBatteryRows, retentionDays);
    }
}