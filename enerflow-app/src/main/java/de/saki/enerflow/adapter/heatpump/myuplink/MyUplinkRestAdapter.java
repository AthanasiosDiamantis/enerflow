package de.saki.enerflow.adapter.heatpump.myuplink;

import de.saki.enerflow.config.MyUplinkProperties;
import de.saki.enerflow.core.model.HeatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Infrastructure adapter that implements {@link HeatGenerator} using the
 * myUplink Cloud REST API. Handles hot water setpoint control for the
 * Novelan Helox 5 heat pump via parameterId 5105.
 *
 * <p>Read operations are intentionally not implemented here —
 * monitoring data is collected by the WebSocket adapter.
 *
 * @author saki
 */
@Component
public class MyUplinkRestAdapter implements HeatGenerator {

    private static final Logger log = LoggerFactory.getLogger(MyUplinkRestAdapter.class);

    /**
     * myUplink parameterId for the domestic hot water setpoint.
     * The API expects the value directly in °C as an integer (e.g. 50 = 50°C).
     */
    private static final String PARAM_HOT_WATER_SETPOINT = "5105";

    private final MyUplinkProperties properties;
    private final MyUplinkTokenService tokenService;
    private final RestClient restClient;

    public MyUplinkRestAdapter(MyUplinkProperties properties,
                               MyUplinkTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.restClient = RestClient.create();
    }

    @Override
    public void setHotWaterSetpoint(double targetTemperatureCelsius) {
        validateSetpoint(targetTemperatureCelsius);

        int apiValue = (int) Math.round(targetTemperatureCelsius);
        String url = properties.getBaseUrl()
                + "/v2/devices/" + properties.getDeviceId() + "/points";

        log.info("Setting hot water setpoint to {} °C", targetTemperatureCelsius);

        try {
            restClient.patch()
                    .uri(url)
                    .header("Authorization", "Bearer " + tokenService.getValidToken())
                    .header("Content-Type", "application/json")
                    .body(Map.of(PARAM_HOT_WATER_SETPOINT, apiValue))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Setpoint successfully updated to {} °C", targetTemperatureCelsius);

        } catch (RestClientException e) {
            log.error("Failed to set hot water setpoint via myUplink: {}", e.getMessage());
            throw new IllegalStateException("myUplink PATCH request failed", e);
        }
    }

    @Override
    public double getHotWaterTemperatureCelsius() {
        throw new UnsupportedOperationException(
                "getHotWaterTemperatureCelsius() is provided by the WebSocket adapter, not myUplink");
    }

    @Override
    public double getHotWaterSetpointCelsius() {
        throw new UnsupportedOperationException(
                "getHotWaterSetpointCelsius() is not implemented in v1.0");
    }

    @Override
    public boolean isAvailable() {
        try {
            tokenService.getValidToken();
            return true;
        } catch (Exception e) {
            log.warn("myUplink availability check failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateSetpoint(double temperatureCelsius) {
        if (temperatureCelsius < properties.getMinSetpointCelsius()
                || temperatureCelsius > properties.getMaxSetpointCelsius()) {
            throw new IllegalArgumentException(
                    "Setpoint %.1f °C is outside the allowed range [%.1f – %.1f °C]".formatted(
                            temperatureCelsius,
                            properties.getMinSetpointCelsius(),
                            properties.getMaxSetpointCelsius()));
        }
    }
}