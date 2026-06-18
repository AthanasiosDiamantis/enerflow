package de.saki.enerflow.core.service;

import de.saki.enerflow.core.domain.ControlAction;
import de.saki.enerflow.core.domain.ControlLog;
import de.saki.enerflow.core.domain.EnerflowState;
import de.saki.enerflow.core.model.EnergySource;
import de.saki.enerflow.core.model.EnergyStorage;
import de.saki.enerflow.core.model.HeatGenerator;
import de.saki.enerflow.core.repository.ControlLogRepository;
import de.saki.enerflow.core.repository.EnerflowStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Core decision logic for EnerFlow's PV-surplus automation.
 * Decides whether the hot water setpoint should be elevated based on
 * battery state of charge and PV surplus, with flapping protection
 * across consecutive polling cycles.
 *
 * <p>Note: heat pump reading and writing are split across two adapters
 * (Novelan WebSocket for reading, myUplink REST for writing) due to a
 * firmware limitation discovered in AP1.1b. Both are injected explicitly
 * via {@code @Qualifier} rather than relying on a single polymorphic
 * {@link HeatGenerator} bean.
 *
 * <p>Every setpoint change (RAISE/RESET) is recorded as an append-only
 * {@link ControlLog} entry, including the estimated thermal energy (kWh)
 * diverted into the hot water tank (AP3.2).
 *
 * @author saki
 */
@Service
@Profile("!test")
public class EnergyManagerService {

    private static final Logger log = LoggerFactory.getLogger(EnergyManagerService.class);

    /** Number of consecutive cycles the condition must hold before acting (flapping protection). */
    private static final int REQUIRED_STABLE_CYCLES = 2;

    private final EnergyStorage energyStorage;
    private final EnergySource energySource;
    private final HeatGenerator heatGeneratorReader;
    private final HeatGenerator heatGeneratorWriter;
    private final DeviceConfigService deviceConfigService;
    private final EnerflowStateRepository stateRepository;
    private final ControlLogRepository controlLogRepository;

    // In-memory flapping-protection counter — intentionally not persisted (see AP2.1 discussion)
    private int consecutiveConditionMetCount = 0;

    public EnergyManagerService(EnergyStorage energyStorage,
                                EnergySource energySource,
                                @Qualifier("novelanHeatpumpClient") HeatGenerator heatGeneratorReader,
                                @Qualifier("myUplinkRestAdapter") HeatGenerator heatGeneratorWriter,
                                DeviceConfigService deviceConfigService,
                                EnerflowStateRepository stateRepository,
                                ControlLogRepository controlLogRepository) {
        this.energyStorage = energyStorage;
        this.energySource = energySource;
        this.heatGeneratorReader = heatGeneratorReader;
        this.heatGeneratorWriter = heatGeneratorWriter;
        this.deviceConfigService = deviceConfigService;
        this.stateRepository = stateRepository;
        this.controlLogRepository = controlLogRepository;
    }

    /**
     * Evaluates current conditions and adjusts the hot water setpoint if necessary.
     * Intended to be called once per polling cycle (60s) by the scheduler (AP2.2).
     */
    public void evaluateAndAct() {
        EnerflowState state = loadState();

        if (!state.isEnabled()) {
            consecutiveConditionMetCount = 0;

            if (state.isBoostActive()) {
                log.info("EnerFlow disabled while boost was active — restoring setpoint");
                deactivateBoost(state);
            } else {
                log.debug("EnerFlow is disabled — skipping evaluation");
            }
            return;
        }

        boolean conditionMet = isSurplusConditionMet();

        if (conditionMet) {
            consecutiveConditionMetCount++;
        } else {
            consecutiveConditionMetCount = 0;
        }

        boolean shouldBoost = consecutiveConditionMetCount >= REQUIRED_STABLE_CYCLES;

        if (shouldBoost && !state.isBoostActive()) {
            activateBoost(state);
        } else if (!shouldBoost && state.isBoostActive()) {
            deactivateBoost(state);
        } else {
            log.debug("No state change — boostActive={}, conditionMet={}, stableCycles={}",
                    state.isBoostActive(), conditionMet, consecutiveConditionMetCount);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isSurplusConditionMet() {
        if (!energyStorage.isAvailable() || !energySource.isAvailable()) {
            log.warn("Energy storage or source unavailable — treating condition as not met");
            return false;
        }

        int soc = energyStorage.getStateOfChargePercent();
        int surplus = energySource.getSurplusWatts();

        int socThreshold = deviceConfigService.getBatterySocThresholdPercent();
        int surplusThreshold = deviceConfigService.getPvSurplusThresholdWatts();

        boolean met = soc >= socThreshold && surplus > surplusThreshold;

        log.debug("Condition check: SOC={}% (threshold {}%), surplus={}W (threshold {}W) -> met={}",
                soc, socThreshold, surplus, surplusThreshold, met);

        return met;
    }

    private void activateBoost(EnerflowState state) {
        if (!heatGeneratorReader.isAvailable() || !heatGeneratorWriter.isAvailable()) {
            log.warn("Heat generator unavailable — cannot activate boost");
            return;
        }

        double currentSetpoint = heatGeneratorReader.getHotWaterSetpointCelsius();
        double elevatedSetpoint = deviceConfigService.getHotwaterSetpointElevatedCelsius();

        log.info("Activating PV-surplus boost: raising setpoint from {}°C to {}°C",
                currentSetpoint, elevatedSetpoint);

        heatGeneratorWriter.setHotWaterSetpoint(elevatedSetpoint);

        state.setBoostActive(true);
        state.setLastKnownSetpoint(currentSetpoint);
        state.setLastUpdated(LocalDateTime.now());
        stateRepository.save(state);

        logControlAction(ControlAction.RAISE, currentSetpoint, elevatedSetpoint);
    }

    private void deactivateBoost(EnerflowState state) {
        if (!heatGeneratorWriter.isAvailable()) {
            log.warn("Heat generator writer unavailable — cannot deactivate boost, will retry next cycle");
            return;
        }

        Double restoreSetpoint = state.getLastKnownSetpoint();
        if (restoreSetpoint == null) {
            log.warn("No last_known_setpoint available — falling back to configured normal setpoint");
            restoreSetpoint = deviceConfigService.getHotwaterSetpointNormalCelsius();
        }

        log.info("Deactivating PV-surplus boost: restoring setpoint to {}°C", restoreSetpoint);

        heatGeneratorWriter.setHotWaterSetpoint(restoreSetpoint);

        // Assumption: the setpoint while boost was active still matches the
        // configured elevated value (no re-read from hardware here to avoid
        // an extra, potentially failing, dependency on heatGeneratorReader).
        double assumedOldSetpoint = deviceConfigService.getHotwaterSetpointElevatedCelsius();
        logControlAction(ControlAction.RESET, assumedOldSetpoint, restoreSetpoint);

        state.setBoostActive(false);
        state.setLastKnownSetpoint(null);
        state.setLastUpdated(LocalDateTime.now());
        stateRepository.save(state);
    }

    private void logControlAction(ControlAction action, double oldSetpointCelsius, double newSetpointCelsius) {
        double deltaCelsius = newSetpointCelsius - oldSetpointCelsius;
        double tankVolumeLiters = deviceConfigService.getHotwaterTankVolumeLiters();
        double kwh = HotWaterEnergyCalculator.calculateKwh(tankVolumeLiters, deltaCelsius);

        ControlLog entry = new ControlLog();
        entry.setTimestamp(LocalDateTime.now());
        entry.setAction(action);
        entry.setOldSetpointCelsius(oldSetpointCelsius);
        entry.setNewSetpointCelsius(newSetpointCelsius);
        entry.setBerechnungKwh(kwh);
        entry.setTriggeredBy("SYSTEM");

        controlLogRepository.save(entry);

        log.debug("Control log entry saved: action={}, {}°C -> {}°C, {} kWh",
                action, oldSetpointCelsius, newSetpointCelsius, kwh);
    }

    private EnerflowState loadState() {
        return stateRepository.findById(1)
                .orElseThrow(() -> new IllegalStateException(
                        "enerflow_state row with id=1 not found — check V3 migration"));
    }
}