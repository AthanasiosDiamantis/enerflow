package de.saki.enerflow.adapter.battery.sonnen;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the JSON response from the sonnenBatterie /api/v2/status endpoint.
 * Only fields relevant for EnerFlow are mapped — the API returns additional
 * fields (voltages, frequencies, etc.) which are not needed here.
 *
 * @author saki
 */
public record SonnenStatusResponse(

        @JsonProperty("USOC") int usableStateOfChargePercent,

        @JsonProperty("Production_W") int productionWatts,

        @JsonProperty("Consumption_W") int consumptionWatts,

        @JsonProperty("Pac_total_W") int batteryPowerWatts,

        @JsonProperty("BatteryCharging") boolean batteryCharging,

        @JsonProperty("BatteryDischarging") boolean batteryDischarging
) {
}