package de.saki.enerflow.core.service;

import de.saki.enerflow.core.domain.EnerflowState;
import de.saki.enerflow.core.model.EnergySource;
import de.saki.enerflow.core.model.EnergyStorage;
import de.saki.enerflow.core.model.HeatGenerator;
import de.saki.enerflow.core.repository.EnerflowStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EnergyManagerService}.
 * All dependencies are mocked — no real hardware or database involved.
 *
 * @author saki
 */
@ExtendWith(MockitoExtension.class)
class EnergyManagerServiceTest {

    @Mock
    private EnergyStorage energyStorage;

    @Mock
    private EnergySource energySource;

    @Mock
    private HeatGenerator heatGeneratorReader;

    @Mock
    private HeatGenerator heatGeneratorWriter;

    @Mock
    private DeviceConfigService deviceConfigService;

    @Mock
    private EnerflowStateRepository stateRepository;

    private EnergyManagerService service;
    private EnerflowState state;

    @BeforeEach
    void setUp() {
        service = new EnergyManagerService(
                energyStorage, energySource, heatGeneratorReader, heatGeneratorWriter,
                deviceConfigService, stateRepository);

        state = new EnerflowState();
        state.setId(1);
        state.setEnabled(true);
        state.setBoostActive(false);
        state.setLastKnownSetpoint(null);

        when(stateRepository.findById(1)).thenReturn(Optional.of(state));

        // Default thresholds — overridden in specific tests if needed
        lenient().when(deviceConfigService.getBatterySocThresholdPercent()).thenReturn(90);
        lenient().when(deviceConfigService.getPvSurplusThresholdWatts()).thenReturn(300);
        lenient().when(deviceConfigService.getHotwaterSetpointElevatedCelsius()).thenReturn(55.0);
        lenient().when(deviceConfigService.getHotwaterSetpointNormalCelsius()).thenReturn(48.0);
    }

    @Test
    @DisplayName("Disabled state with no active boost: no action taken")
    void evaluateAndAct_disabledNoBoostActive_doesNothing() {
        state.setEnabled(false);
        state.setBoostActive(false);

        service.evaluateAndAct();

        verify(heatGeneratorWriter, never()).setHotWaterSetpoint(anyDouble());
        verify(stateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Disabled while boost was active: setpoint is restored")
    void evaluateAndAct_disabledWhileBoostActive_restoresSetpoint() {
        state.setEnabled(false);
        state.setBoostActive(true);
        state.setLastKnownSetpoint(48.0);
        when(heatGeneratorWriter.isAvailable()).thenReturn(true);

        service.evaluateAndAct();

        verify(heatGeneratorWriter).setHotWaterSetpoint(48.0);
        assertThat(state.isBoostActive()).isFalse();
    }

    @Test
    @DisplayName("Conditions met for only 1 cycle: flapping protection prevents activation")
    void evaluateAndAct_conditionMetOnce_doesNotActivateYet() {
        givenConditionsAreMet();

        service.evaluateAndAct();

        verify(heatGeneratorWriter, never()).setHotWaterSetpoint(anyDouble());
        assertThat(state.isBoostActive()).isFalse();
    }

    @Test
    @DisplayName("Conditions met for 2 consecutive cycles: boost activates and last_known_setpoint is saved")
    void evaluateAndAct_conditionMetTwice_activatesBoost() {
        givenConditionsAreMet();
        when(heatGeneratorReader.isAvailable()).thenReturn(true);
        when(heatGeneratorWriter.isAvailable()).thenReturn(true);
        when(heatGeneratorReader.getHotWaterSetpointCelsius()).thenReturn(48.0);

        service.evaluateAndAct(); // cycle 1
        service.evaluateAndAct(); // cycle 2 - should activate

        verify(heatGeneratorWriter).setHotWaterSetpoint(55.0);
        assertThat(state.isBoostActive()).isTrue();
        assertThat(state.getLastKnownSetpoint()).isEqualTo(48.0);
        verify(stateRepository, atLeastOnce()).save(state);
    }

    @Test
    @DisplayName("Condition drops after 1 stable cycle: counter resets, no activation")
    void evaluateAndAct_conditionDropsBeforeThreshold_resetsCounter() {
        givenConditionsAreMet();

        service.evaluateAndAct(); // cycle 1 - condition met

        givenConditionsAreNotMet();
        service.evaluateAndAct(); // cycle 2 - condition not met, counter resets

        givenConditionsAreMet();
        service.evaluateAndAct(); // cycle 3 - met again, but counter restarted at 1

        verify(heatGeneratorWriter, never()).setHotWaterSetpoint(anyDouble());
    }

    @Test
    @DisplayName("Boost active and condition no longer met: setpoint restored to last_known_setpoint")
    void evaluateAndAct_boostActiveConditionNotMet_restoresSetpoint() {
        state.setBoostActive(true);
        state.setLastKnownSetpoint(48.0);
        when(heatGeneratorWriter.isAvailable()).thenReturn(true);

        givenConditionsAreNotMet();
        service.evaluateAndAct();

        verify(heatGeneratorWriter).setHotWaterSetpoint(48.0);
        assertThat(state.isBoostActive()).isFalse();
        assertThat(state.getLastKnownSetpoint()).isNull();
    }

    @Test
    @DisplayName("Boost active, no last_known_setpoint available: falls back to configured normal setpoint")
    void evaluateAndAct_boostActiveNoLastKnownSetpoint_usesConfiguredFallback() {
        state.setBoostActive(true);
        state.setLastKnownSetpoint(null);
        when(heatGeneratorWriter.isAvailable()).thenReturn(true);

        givenConditionsAreNotMet();
        service.evaluateAndAct();

        verify(heatGeneratorWriter).setHotWaterSetpoint(48.0); // configured normal setpoint
    }

    @Test
    @DisplayName("Energy source unavailable: condition treated as not met, no exception thrown")
    void evaluateAndAct_energySourceUnavailable_treatsAsConditionNotMet() {
        when(energyStorage.isAvailable()).thenReturn(true);
        when(energySource.isAvailable()).thenReturn(false);

        service.evaluateAndAct();

        verify(heatGeneratorWriter, never()).setHotWaterSetpoint(anyDouble());
    }

    @Test
    @DisplayName("Heat generator unavailable during activation attempt: boost is not activated")
    void evaluateAndAct_heatGeneratorUnavailableDuringActivation_doesNotActivate() {
        givenConditionsAreMet();
        when(heatGeneratorReader.isAvailable()).thenReturn(false);

        service.evaluateAndAct(); // cycle 1
        service.evaluateAndAct(); // cycle 2 - threshold reached, but heat generator unavailable

        verify(heatGeneratorWriter, never()).setHotWaterSetpoint(anyDouble());
        assertThat(state.isBoostActive()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private void givenConditionsAreMet() {
        when(energyStorage.isAvailable()).thenReturn(true);
        when(energySource.isAvailable()).thenReturn(true);
        when(energyStorage.getStateOfChargePercent()).thenReturn(95);
        when(energySource.getSurplusWatts()).thenReturn(500);
    }

    private void givenConditionsAreNotMet() {
        when(energyStorage.isAvailable()).thenReturn(true);
        when(energySource.isAvailable()).thenReturn(true);
        when(energyStorage.getStateOfChargePercent()).thenReturn(50);
        when(energySource.getSurplusWatts()).thenReturn(100);
    }
}