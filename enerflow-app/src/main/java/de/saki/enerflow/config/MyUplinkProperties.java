package de.saki.enerflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the myUplink Cloud REST API.
 * Values are located in application.yaml under the "myuplink" prefix.
 *
 * @author saki
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "myuplink")
public class MyUplinkProperties {

    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String deviceId;

    /** Minimum remaining token lifetime in seconds before proactive refresh. */
    private int tokenRefreshThresholdSeconds = 60;

    /** Safety range for hot water setpoint (the device supports 30–65 °C). */
    private double minSetpointCelsius = 30.0;
    private double maxSetpointCelsius = 65.0;
}