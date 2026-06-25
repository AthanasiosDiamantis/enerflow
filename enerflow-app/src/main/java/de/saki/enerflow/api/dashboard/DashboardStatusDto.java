package de.saki.enerflow.api.dashboard;

import java.time.LocalDateTime;

/**
 * DTO for the dashboard status endpoint (GET /api/dashboard/status).
 * Aggregates data from battery_snapshot, heatpump_snapshot, enerflow_state
 * and today's control_log entries into a single JSON response.
 *
 * Consumed by the frontend every 10 seconds via fetch().
 */
public record DashboardStatusDto(

        // --- PV & Battery (source: battery_snapshot) ---

        /* PV production in watts. */
        int pvProductionW,

        /* Battery state of charge in percent (0–100). */
        int batteryChargePercent,

        /**
         * Color code for the battery indicator.
         * "green"  = SOC >= 50%
         * "yellow" = SOC >= 20%
         * "red"    = SOC < 20%
         */
        String batteryColorCode,

        /* True if the battery is currently charging. */
        boolean batteryCharging,

        /* True if the battery is currently discharging. */
        boolean batteryDischarging,

        // --- Grid & Consumption (source: battery_snapshot) ---

        /**
         * Grid feed-in in watts.
         * Positive = feeding into grid, negative = drawing from grid.
         */
        int gridFeedInW,

        /* Household consumption in watts. */
        int consumptionW,

        // --- Heat pump (source: heatpump_snapshot) ---

        /**
         * Operating status of the heat pump.
         * Expected values: "HEATING", "HOT_WATER", "STANDBY", "OFF"
         */
        String heatPumpStatus,

        /* Current hot water temperature in °C (Ist-Temperatur). */
        double hotWaterTempActual,

        /* Current hot water setpoint in °C (Soll-Temperatur). */
        double hotWaterTempSetpoint,

        // --- EnerFlow state (source: enerflow_state) ---

        /* True if EnerFlow automation is enabled. */
        boolean enerflowActive,

        /**
         * True if EnerFlow has currently raised the setpoint above
         * last_known_setpoint (i.e. boost mode is active).
         * Used by the frontend to pulse the heat pump icon red (US-01-02).
         */
        boolean boostActive,

        // --- Savings today (source: control_log, aggregated) ---

        /* Kilowatt-hours saved today through EnerFlow excess control. */
        double savedKwhToday,

        /* Money saved today in euros (savedKwhToday × electricityPriceCentPerKwh / 100). */
        double savedEuroToday,

        /**
         * Estimated number of showers possible today without the heat pump
         * needing to run. Based on saved kWh and a fixed 0.35 kWh per shower.
         */
        int possibleShowersToday,

        // --- Configuration (source: device_config) ---

        /* Electricity price in cent per kWh, configured by the user (US-01-04). */
        double electricityPriceCentPerKwh,

        // --- Meta ---

        /* Timestamp of the most recent battery_snapshot (or heatpump_snapshot). */
        LocalDateTime lastSnapshotTime,

        /**
         * Data freshness indicator.
         * "FRESH" = last snapshot < 2 minutes ago (within 2 polling cycles)
         * "STALE" = last snapshot >= 2 minutes ago (possible connection issue)
         */
        String dataFreshness

) {
    // Compact constructor for input validation
    public DashboardStatusDto {
        if (batteryChargePercent < 0 || batteryChargePercent > 100) {
            throw new IllegalArgumentException(
                    "batteryChargePercent must be between 0 and 100, got: " + batteryChargePercent
            );
        }
        if (electricityPriceCentPerKwh < 0) {
            throw new IllegalArgumentException(
                    "electricityPriceCentPerKwh must not be negative"
            );
        }
    }

    /**
     * Factory method to derive batteryColorCode from SOC percentage.
     * Keeps the color logic central – frontend just reads the string.
     */
    public static String deriveBatteryColorCode(int socPercent) {
        if (socPercent >= 50) return "green";
        if (socPercent >= 20) return "yellow";
        return "red";
    }

    /**
     * Convenience: PV production in kilowatts, rounded to 2 decimal places.
     * Added as a derived method, so the JSON keeps pvProductionW as the
     * source of truth, but the frontend can also read kW if it needs it.
     */
    public double pvProductionKw() {
        return Math.round(pvProductionW / 10.0) / 100.0;
    }
}