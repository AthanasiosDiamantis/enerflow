package de.saki.enerflow.core.service;

import de.saki.enerflow.core.repository.DeviceConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides typed access to runtime-configurable EnerFlow parameters
 * stored in the device_config table.
 *
 * @author saki
 */
@Service
public class DeviceConfigService {

    private static final Logger log = LoggerFactory.getLogger(DeviceConfigService.class);

    // Config keys, defined as constants to avoid magic strings
    public static final String KEY_PV_SURPLUS_THRESHOLD = "pv.surplus.threshold.watts";
    public static final String KEY_BATTERY_SOC_THRESHOLD = "battery.soc.threshold.percent";
    public static final String KEY_HOTWATER_SETPOINT_ELEVATED = "hotwater.setpoint.elevated.celsius";
    public static final String KEY_HOTWATER_SETPOINT_NORMAL = "hotwater.setpoint.normal.celsius";
    public static final String KEY_TANK_VOLUME_LITERS = "hotwater.tank.volume.liters";
    public static final String KEY_SNAPSHOT_RETENTION_DAYS = "data.retention.snapshot.days";

    private final DeviceConfigRepository repository;

    public DeviceConfigService(DeviceConfigRepository repository) {
        this.repository = repository;
    }

    public int getPvSurplusThresholdWatts() {
        return getInt(KEY_PV_SURPLUS_THRESHOLD, 300);
    }

    public int getBatterySocThresholdPercent() {
        return getInt(KEY_BATTERY_SOC_THRESHOLD, 90);
    }

    public double getHotwaterSetpointElevatedCelsius() {
        return getDouble(KEY_HOTWATER_SETPOINT_ELEVATED, 55.0);
    }

    public double getHotwaterSetpointNormalCelsius() {
        return getDouble(KEY_HOTWATER_SETPOINT_NORMAL, 48.0);
    }

    public double getHotwaterTankVolumeLiters() {
        return getDouble(KEY_TANK_VOLUME_LITERS, 300.0);
    }

    public int getSnapshotRetentionDays() {
        return getInt(KEY_SNAPSHOT_RETENTION_DAYS, 730);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private int getInt(String key, int defaultValue) {
        return repository.findById(key)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getConfigValue());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid integer value for config key '{}': '{}' — using default {}",
                                key, c.getConfigValue(), defaultValue);
                        return defaultValue;
                    }
                })
                .orElseGet(() -> {
                    log.warn("Config key '{}' not found — using default {}", key, defaultValue);
                    return defaultValue;
                });
    }

    private double getDouble(String key, double defaultValue) {
        return repository.findById(key)
                .map(c -> {
                    try {
                        return Double.parseDouble(c.getConfigValue());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid double value for config key '{}': '{}' — using default {}",
                                key, c.getConfigValue(), defaultValue);
                        return defaultValue;
                    }
                })
                .orElseGet(() -> {
                    log.warn("Config key '{}' not found — using default {}", key, defaultValue);
                    return defaultValue;
                });
    }


}
