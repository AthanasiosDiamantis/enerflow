package de.saki.enerflow.api.dashboard;

import de.saki.enerflow.core.domain.*;
import de.saki.enerflow.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private BatterySnapshotRepository batterySnapshotRepository;

    @Mock
    private HeatpumpSnapshotRepository heatpumpSnapshotRepository;

    @Mock
    private EnerflowStateRepository enerflowStateRepository;

    @Mock
    private ControlLogRepository controlLogRepository;

    @Mock
    private ElectricityPriceRepository electricityPriceRepository;

    @InjectMocks
    private DashboardService service;

    private BatterySnapshot battery;
    private HeatpumpSnapshot heatpump;
    private EnerflowState state;

    @BeforeEach
    void setUp() {
        battery = new BatterySnapshot();
        battery.setTimestamp(LocalDateTime.now());
        battery.setUsableSocPercent(75);
        battery.setProductionWatts(3000);
        battery.setConsumptionWatts(500);
        battery.setBatteryPowerWatts(1200); // Charging with 1200 watts
        battery.setBatteryCharging(true);
        battery.setBatteryDischarging(false);
        battery.setSurplusWatts(1300);

        heatpump = new HeatpumpSnapshot();
        heatpump.setWarmwasserIst(47.5);
        heatpump.setWarmwasserSoll(55.0);
        heatpump.setBetriebszustand("HOT_WATER");

        state = new EnerflowState();
        state.setId(1);
        state.setEnabled(true);
        state.setBoostActive(true);
        state.setLastKnownSetpoint(47.0);
        state.setLastUpdated(LocalDateTime.now());

        // Default stubbing shared by most tests — happy path.
        // Individual tests override what they need via additional when(...) calls.
        lenient().when(batterySnapshotRepository.findTopByOrderByTimestampDesc())
                .thenReturn(Optional.of(battery));
        lenient().when(heatpumpSnapshotRepository.findTopByOrderByTimestampDesc())
                .thenReturn(Optional.of(heatpump));
        lenient().when(enerflowStateRepository.findById(1))
                .thenReturn(Optional.of(state));
        lenient().when(controlLogRepository.findByTimestampBetweenOrderByTimestampDesc(any(), any()))
                .thenReturn(List.of());
        lenient().when(electricityPriceRepository.findById(1))
                .thenReturn(Optional.empty());

    }

    @Test
    @DisplayName("assembles DTO with correctly mapped battery, heat pump and state values")
    void assemblesDto_withCorrectlyMappedValues() {
        DashboardStatusDto dto = service.getDashboardStatus();

        assertThat(dto.pvProductionW()).isEqualTo(3000);
        assertThat(dto.batteryChargePercent()).isEqualTo(75);
        assertThat(dto.batteryColorCode()).isEqualTo("green");
        assertThat(dto.batteryCharging()).isTrue();
        assertThat(dto.batteryDischarging()).isFalse();
        assertThat(dto.consumptionW()).isEqualTo(500);
        assertThat(dto.heatPumpStatus()).isEqualTo("HOT_WATER");
        assertThat(dto.hotWaterTempActual()).isEqualTo(47.5);
        assertThat(dto.hotWaterTempSetpoint()).isEqualTo(55.0);
        assertThat(dto.enerflowActive()).isTrue();
        assertThat(dto.boostActive()).isTrue();
    }

    @Test
    @DisplayName("derives grid feed-in as production minus consumption minus battery power")
    void derivesGridFeedIn_fromEnergyBalance() {
        // production=3000, consumption=500, batteryPower=1200 (charging)
        // grid = 3000 - 500 - 1200 = 1300
        DashboardStatusDto dto = service.getDashboardStatus();

        assertThat(dto.gridFeedInW()).isEqualTo(1300);
    }

    @Test
    @DisplayName("derives battery color code as yellow between 20% and 49% SOC")
    void derivesBatteryColorCode_yellow_forMidRangeSoc() {
        battery.setUsableSocPercent(35);

        DashboardStatusDto dto = service.getDashboardStatus();

        assertThat(dto.batteryColorCode()).isEqualTo("yellow");
    }

    @Test
    @DisplayName("derives battery color code as red below 20% SOC")
    void derivesBatteryColorCode_red_forLowSoc() {
        battery.setUsableSocPercent(10);

        DashboardStatusDto dto = service.getDashboardStatus();

        assertThat(dto.batteryColorCode()).isEqualTo("red");
    }

    @Test
    @DisplayName("marks data as FRESH when the latest snapshot is less than 2 minutes old")
    void marksDataFreshness_asFresh_whenSnapshotIsRecent() {
        battery.setTimestamp(LocalDateTime.now().minusSeconds(30));

        DashboardStatusDto dto = service.getDashboardStatus();

        assertThat(dto.dataFreshness()).isEqualTo("FRESH");
    }

    @Test
    @DisplayName("marks data as STALE when the latest snapshot is 2 minutes old or more")
    void marksDataFreshness_asStale_whenSnapshotIsOld() {
        battery.setTimestamp(LocalDateTime.now().minusMinutes(5));

        DashboardStatusDto dto = service.getDashboardStatus();

        assertThat(dto.dataFreshness()).isEqualTo("STALE");
    }

    @Test
    @DisplayName("calculates saved kWh and showers from a closed RAISE/RESET interval")
    void calculatesSavedKwh_fromClosedRaiseResetInterval() {
        LocalDateTime raisedAt = LocalDateTime.now().minusHours(1);
        LocalDateTime resetAt = LocalDateTime.now();

        ControlLog raise = new ControlLog();
        raise.setAction(ControlAction.RAISE);
        raise.setTimestamp(raisedAt);

        ControlLog reset = new ControlLog();
        reset.setAction(ControlAction.RESET);
        reset.setTimestamp(resetAt);

        // Repository returns entries ordered DESC (most recent first), as the real query does.
        when(controlLogRepository.findByTimestampBetweenOrderByTimestampDesc(any(), any()))
                .thenReturn(List.of(reset, raise));

        DashboardStatusDto dto = service.getDashboardStatus();

        // 1 hour boost * 1.5 kW heat pump power = 1.5 kWh
        assertThat(dto.savedKwhToday()).isEqualTo(1.5);
        // 1.5 kWh / 0.35 kWh per shower = 4 showers (int division)
        assertThat(dto.possibleShowersToday()).isEqualTo(4);
    }

    @Test
    @DisplayName("counts an ongoing boost (RAISE without RESET) until now")
    void calculatesSavedKwh_forOngoingBoostWithoutReset() {
        LocalDateTime raisedAt = LocalDateTime.now().minusMinutes(30);

        ControlLog raise = new ControlLog();
        raise.setAction(ControlAction.RAISE);
        raise.setTimestamp(raisedAt);

        when(controlLogRepository.findByTimestampBetweenOrderByTimestampDesc(any(), any()))
                .thenReturn(List.of(raise));

        DashboardStatusDto dto = service.getDashboardStatus();

        // 0.5 hour boost * 1.5 kW = 0.75 kWh
        assertThat(dto.savedKwhToday()).isEqualTo(0.75);
    }

    @Test
    @DisplayName("computes euro savings using the configured electricity price")
    void computesEuroSavings_usingConfiguredPrice() {
        ElectricityPriceConfig priceConfig = new ElectricityPriceConfig();
        priceConfig.setPriceCentPerKwh(30.0);
        when(electricityPriceRepository.findById(1)).thenReturn(Optional.of(priceConfig));

        LocalDateTime raisedAt = LocalDateTime.now().minusHours(1);
        LocalDateTime resetAt = LocalDateTime.now();
        ControlLog raise = new ControlLog();
        raise.setAction(ControlAction.RAISE);
        raise.setTimestamp(raisedAt);
        ControlLog reset = new ControlLog();
        reset.setAction(ControlAction.RESET);
        reset.setTimestamp(resetAt);
        when(controlLogRepository.findByTimestampBetweenOrderByTimestampDesc(any(), any()))
                .thenReturn(List.of(reset, raise));

        DashboardStatusDto dto = service.getDashboardStatus();

        // 1.5 kWh saved * 30 ct/kWh / 100 = 0.45 EUR
        assertThat(dto.savedEuroToday()).isEqualTo(0.45);
        assertThat(dto.electricityPriceCentPerKwh()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("falls back to default price of 28.0 ct/kWh when no price config exists")
    void fallsBackToDefaultPrice_whenNoPriceConfigExists() {
        // electricityPriceRepository.findById(1) already stubbed to Optional.empty() in setUp()
        DashboardStatusDto dto = service.getDashboardStatus();

        assertThat(dto.electricityPriceCentPerKwh()).isEqualTo(28.0);
    }

    @Test
    @DisplayName("throws IllegalStateException when no battery snapshot exists")
    void throwsException_whenNoBatterySnapshotExists() {
        when(batterySnapshotRepository.findTopByOrderByTimestampDesc())
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDashboardStatus())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("battery snapshot");
    }

    @Test
    @DisplayName("throws IllegalStateException when no heat pump snapshot exists")
    void throwsException_whenNoHeatpumpSnapshotExists() {
        when(heatpumpSnapshotRepository.findTopByOrderByTimestampDesc())
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDashboardStatus())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("heatpump snapshot");
    }

    @Test
    @DisplayName("throws IllegalStateException when the enerflow_state singleton is missing")
    void throwsException_whenEnerflowStateMissing() {
        when(enerflowStateRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDashboardStatus())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EnerFlow state");
    }



}