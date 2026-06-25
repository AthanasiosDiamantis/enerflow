package de.saki.enerflow.api.dashboard;

import de.saki.enerflow.core.domain.BatterySnapshot;
import de.saki.enerflow.core.domain.ControlAction;
import de.saki.enerflow.core.domain.ControlLog;
import de.saki.enerflow.core.domain.HeatpumpSnapshot;
import de.saki.enerflow.core.repository.BatterySnapshotRepository;
import de.saki.enerflow.core.repository.ControlLogRepository;
import de.saki.enerflow.core.repository.EnerflowStateRepository;
import de.saki.enerflow.core.repository.HeatpumpSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for assembling the dashboard status DTO.
 * Reads the latest snapshots, the EnerFlow state, and today's control log
 * entries, then aggregates them into a single {@link DashboardStatusDto}.
 *
 * @author saki
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /**
     * Estimated heat pump power during hot water heating in kW.
     */
    private static final double HEAT_PUMP_POWER_KW = 1.5;

    /**
     * Energy required for one shower in kWh (approx. 50L, ΔT = 30°C).
     */
    private static final double KWH_PER_SHOWER = 0.35;

    /**
     * A snapshot older than this is considered stale.
     */
    private static final long STALE_THRESHOLD_MINUTES = 2;

    /**
     * Default electricity price if no config entry exists in DB yet.
     */
    private static final double DEFAULT_ELECTRICITY_PRICE_CENT = 28.0;

    // -----------------------------------------------------------------------
    // Repositories
    // -----------------------------------------------------------------------

    private final BatterySnapshotRepository batterySnapshotRepository;
    private final HeatpumpSnapshotRepository heatpumpSnapshotRepository;
    private final EnerflowStateRepository enerflowStateRepository;
    private final ControlLogRepository controlLogRepository;

    public DashboardService(
            BatterySnapshotRepository batterySnapshotRepository,
            HeatpumpSnapshotRepository heatpumpSnapshotRepository,
            EnerflowStateRepository enerflowStateRepository,
            ControlLogRepository controlLogRepository) {

        this.batterySnapshotRepository = batterySnapshotRepository;
        this.heatpumpSnapshotRepository = heatpumpSnapshotRepository;
        this.enerflowStateRepository = enerflowStateRepository;
        this.controlLogRepository = controlLogRepository;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Assembles and returns the current dashboard status.
     * Called by the REST controller every time the frontend polls.
     */
    public DashboardStatusDto getDashboardStatus() {

        // 1. Latest battery snapshot
        BatterySnapshot battery = batterySnapshotRepository
                .findTopByOrderByTimestampDesc()
                .orElseThrow(() -> new IllegalStateException(
                        "No battery snapshot found in database"));

        // 2. Latest heatpump snapshot
        HeatpumpSnapshot heatpump = heatpumpSnapshotRepository
                .findTopByOrderByTimestampDesc()
                .orElseThrow(() -> new IllegalStateException(
                        "No heatpump snapshot found in database"));

        // 3. EnerFlow state (singleton, id = 1)
        var state = enerflowStateRepository
                .findById(1)
                .orElseThrow(() -> new IllegalStateException(
                        "EnerFlow state singleton missing in database"));

        // 4. Today's control log entries for savings calculation
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        List<ControlLog> todayLogs = controlLogRepository
                .findByTimestampBetweenOrderByTimestampDesc(startOfDay, now);

        // 5. Battery values
        int soc = battery.getUsableSocPercent();
        int pvW = battery.getProductionWatts();
        int consumptionW = battery.getConsumptionWatts();
        boolean charging = battery.isBatteryCharging();
        boolean discharging = battery.isBatteryDischarging();

        // Grid feed-in derived from energy balance
        // positive = feeding into grid, negative = drawing from grid
        int gridW = battery.getProductionWatts()
                - battery.getConsumptionWatts()
                - battery.getBatteryPowerWatts();

        // 6. Heatpump values
        double tempActual = heatpump.getWarmwasserIst();
        double tempSetpoint = heatpump.getWarmwasserSoll();
        String hpStatus = heatpump.getBetriebszustand();

        // 7. EnerFlow state values
        boolean enerflowActive = state.isEnabled();
        // No longer needed for boostActive derivation – kept for potential future use
        // double lastKnownSetpoint = state.getLastKnownSetpoint();
        boolean boostActive = state.isBoostActive();  // already maintained by EnergyManagerService

        // 8. Savings
        double savedKwhToday = calculateSavedKwhToday(todayLogs);
        double savedEuroToday = savedKwhToday * DEFAULT_ELECTRICITY_PRICE_CENT / 100.0;
        int showers = (int) (savedKwhToday / KWH_PER_SHOWER);

        // 9. Data freshness
        LocalDateTime snapshotTime = battery.getTimestamp();
        String freshness = deriveDataFreshness(snapshotTime);

        // 10. Assemble and return DTO
        return new DashboardStatusDto(
                pvW,
                soc,
                DashboardStatusDto.deriveBatteryColorCode(soc),
                charging,
                discharging,
                gridW,
                consumptionW,
                hpStatus,
                tempActual,
                tempSetpoint,
                enerflowActive,
                boostActive,
                savedKwhToday,
                savedEuroToday,
                showers,
                DEFAULT_ELECTRICITY_PRICE_CENT,
                snapshotTime,
                freshness
        );
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Estimates the kWh saved today by EnerFlow boost activations.
     * Sums ACTIVATED → DEACTIVATED intervals from the control log
     * and multiplies by the estimated heat pump power.
     */
    private double calculateSavedKwhToday(List<ControlLog> logs) {
        double totalHours = 0.0;
        LocalDateTime raisedAt = null;

        for (ControlLog log : logs) {
            if (ControlAction.RAISE == log.getAction()) {
                raisedAt = log.getTimestamp();
            } else if (ControlAction.RESET == log.getAction() && raisedAt != null) {
                long minutes = ChronoUnit.MINUTES.between(raisedAt, log.getTimestamp());
                totalHours += minutes / 60.0;
                raisedAt = null;
            }
        }

        // Boost still active right now – count until this moment
        if (raisedAt != null) {
            long minutes = ChronoUnit.MINUTES.between(raisedAt, LocalDateTime.now());
            totalHours += minutes / 60.0;
        }

        return Math.round(totalHours * HEAT_PUMP_POWER_KW * 100.0) / 100.0;
    }

    /**
     * Returns "FRESH" if the snapshot is younger than the stale threshold,
     * "STALE" otherwise.
     */
    private String deriveDataFreshness(LocalDateTime snapshotTime) {
        long minutesOld = ChronoUnit.MINUTES.between(snapshotTime, LocalDateTime.now());
        return minutesOld < STALE_THRESHOLD_MINUTES ? "FRESH" : "STALE";
    }
}