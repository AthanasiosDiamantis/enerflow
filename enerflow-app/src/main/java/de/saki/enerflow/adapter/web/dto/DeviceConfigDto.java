package de.saki.enerflow.adapter.web.dto;

/**
 * Data transfer object for reading and updating runtime-configurable
 * EnerFlow parameters (device_config table) via the Manager UI.
 *
 * @author saki
 */
public record DeviceConfigDto(
        int pvSurplusThresholdWatts,
        int batterySocThresholdPercent,
        double hotwaterSetpointElevatedCelsius,
        double hotwaterSetpointNormalCelsius,
        double hotwaterTankVolumeLiters,
        int snapshotRetentionDays
) {
}