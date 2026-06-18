package de.saki.enerflow.adapter.battery.sonnen;

import de.saki.enerflow.config.SonnenBatteryProperties;
import de.saki.enerflow.core.model.EnergySource;
import de.saki.enerflow.core.model.EnergyStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Infrastructure adapter for the sonnenBatterie local REST API.
 * Implements both {@link EnergyStorage} (battery state) and
 * {@link EnergySource} (PV production and surplus) since both
 * are delivered by the same /api/v2/status endpoint.
 *
 * @author saki
 */
@Component
public class SonnenBatteryAdapter implements EnergyStorage, EnergySource {

    private static final Logger log = LoggerFactory.getLogger(SonnenBatteryAdapter.class);

    private final SonnenBatteryProperties properties;
    private final RestClient restClient;

    public SonnenBatteryAdapter(SonnenBatteryProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    // -------------------------------------------------------------------------
    // EnergyStorage
    // -------------------------------------------------------------------------

    @Override
    public int getStateOfChargePercent() {
        return fetchStatus().usableStateOfChargePercent();
    }

    @Override
    public boolean isCharging() {
        return fetchStatus().batteryCharging();
    }

    @Override
    public boolean isDischarging() {
        return fetchStatus().batteryDischarging();
    }

    // -------------------------------------------------------------------------
    // EnergySource
    // -------------------------------------------------------------------------

    @Override
    public int getProductionWatts() {
        return fetchStatus().productionWatts();
    }

    @Override
    public int getSurplusWatts() {
        SonnenStatusResponse status = fetchStatus();

        // Pac_total_W is negative while charging, positive while discharging.
        // Battery charging power is therefore the negative part of Pac_total_W.
        int batteryChargingWatts = Math.max(0, -status.batteryPowerWatts());

        return status.productionWatts() - status.consumptionWatts() - batteryChargingWatts;
    }

    /**
     * Exposes the raw status response for components that need the full
     * data set (e.g. {@link SonnenBatterySnapshotService}), not just the
     * generic EnergyStorage/EnergySource view.
     */
    public SonnenStatusResponse fetchCurrentStatus() {
        return fetchStatus();
    }

    // -------------------------------------------------------------------------
    // Shared availability check
    // -------------------------------------------------------------------------

    @Override
    public boolean isAvailable() {
        try {
            fetchStatus();
            return true;
        } catch (Exception e) {
            log.warn("sonnenBatterie availability check failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private SonnenStatusResponse fetchStatus() {
        String url = "http://" + properties.getHost() + "/api/v2/status";

        try {
            SonnenStatusResponse response = restClient.get()
                    .uri(url)
                    .header("Auth-Token", properties.getApiToken())
                    .retrieve()
                    .body(SonnenStatusResponse.class);

            if (response == null) {
                throw new IllegalStateException("sonnenBatterie returned empty response");
            }
            return response;

        } catch (RestClientException e) {
            log.error("Failed to fetch sonnenBatterie status: {}", e.getMessage());
            throw new IllegalStateException("sonnenBatterie status request failed", e);
        }
    }
}