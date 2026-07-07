package de.saki.enerflow.core.service;

import de.saki.enerflow.core.domain.DeviceConfig;
import de.saki.enerflow.core.repository.DeviceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceConfigServiceTest {

    @Mock
    private DeviceConfigRepository repository;

    private DeviceConfigService service;

    @BeforeEach
    void setUp() {
        service = new DeviceConfigService(repository);

        // No config keys exist by default — each test overrides what it needs.
        // lenient() because not every test looks up every key.
        lenient().when(repository.findById(anyString()))
                .thenReturn(Optional.empty());
    }

    private DeviceConfig configWithValue(String key, String value) {
        DeviceConfig config = new DeviceConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setLastUpdated(LocalDateTime.now());
        return config;
    }

    // -------------------------------------------------------------------
    // Fallback to defaults when a key is missing
    // -------------------------------------------------------------------

    @Test
    @DisplayName("falls back to default 300W when PV surplus threshold is not configured")
    void getPvSurplusThresholdWatts_fallsBackToDefault_whenKeyMissing() {
        assertThat(service.getPvSurplusThresholdWatts()).isEqualTo(300);
    }

    @Test
    @DisplayName("falls back to default 90% when battery SOC threshold is not configured")
    void getBatterySocThresholdPercent_fallsBackToDefault_whenKeyMissing() {
        assertThat(service.getBatterySocThresholdPercent()).isEqualTo(90);
    }

    @Test
    @DisplayName("falls back to default 55.0°C when elevated hot water setpoint is not configured")
    void getHotwaterSetpointElevatedCelsius_fallsBackToDefault_whenKeyMissing() {
        assertThat(service.getHotwaterSetpointElevatedCelsius()).isEqualTo(55.0);
    }

    @Test
    @DisplayName("falls back to default 48.0°C when normal hot water setpoint is not configured")
    void getHotwaterSetpointNormalCelsius_fallsBackToDefault_whenKeyMissing() {
        assertThat(service.getHotwaterSetpointNormalCelsius()).isEqualTo(48.0);
    }

    @Test
    @DisplayName("falls back to default 300.0L when tank volume is not configured")
    void getHotwaterTankVolumeLiters_fallsBackToDefault_whenKeyMissing() {
        assertThat(service.getHotwaterTankVolumeLiters()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("falls back to default 730 days when snapshot retention is not configured")
    void getSnapshotRetentionDays_fallsBackToDefault_whenKeyMissing() {
        assertThat(service.getSnapshotRetentionDays()).isEqualTo(730);
    }

    // -------------------------------------------------------------------
    // Reading a configured (valid) value
    // -------------------------------------------------------------------

    @Test
    @DisplayName("returns the configured integer value when present")
    void getInt_returnsConfiguredValue_whenPresentAndValid() {
        when(repository.findById(DeviceConfigService.KEY_PV_SURPLUS_THRESHOLD))
                .thenReturn(Optional.of(configWithValue(DeviceConfigService.KEY_PV_SURPLUS_THRESHOLD, "450")));

        assertThat(service.getPvSurplusThresholdWatts()).isEqualTo(450);
    }

    @Test
    @DisplayName("returns the configured double value when present")
    void getDouble_returnsConfiguredValue_whenPresentAndValid() {
        when(repository.findById(DeviceConfigService.KEY_HOTWATER_SETPOINT_ELEVATED))
                .thenReturn(Optional.of(configWithValue(DeviceConfigService.KEY_HOTWATER_SETPOINT_ELEVATED, "58.5")));

        assertThat(service.getHotwaterSetpointElevatedCelsius()).isEqualTo(58.5);
    }

    // -------------------------------------------------------------------
    // Falling back to defaults on malformed values (not just missing keys)
    // -------------------------------------------------------------------

    @Test
    @DisplayName("falls back to default when the stored integer value is not parseable")
    void getInt_fallsBackToDefault_whenValueIsMalformed() {
        when(repository.findById(DeviceConfigService.KEY_BATTERY_SOC_THRESHOLD))
                .thenReturn(Optional.of(configWithValue(DeviceConfigService.KEY_BATTERY_SOC_THRESHOLD, "not-a-number")));

        assertThat(service.getBatterySocThresholdPercent()).isEqualTo(90);
    }

    @Test
    @DisplayName("falls back to default when the stored double value is not parseable")
    void getDouble_fallsBackToDefault_whenValueIsMalformed() {
        when(repository.findById(DeviceConfigService.KEY_TANK_VOLUME_LITERS))
                .thenReturn(Optional.of(configWithValue(DeviceConfigService.KEY_TANK_VOLUME_LITERS, "abc")));

        assertThat(service.getHotwaterTankVolumeLiters()).isEqualTo(300.0);
    }
}