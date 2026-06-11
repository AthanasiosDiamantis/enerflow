package de.saki.enerflow.adapter.heatpump.myuplink;

import de.saki.enerflow.config.MyUplinkProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * Integration test for {@link MyUplinkRestAdapter}.
 * Makes real HTTP calls to the myUplink Cloud API.
 *
 * <p>Requires valid credentials in the .env file:
 * MYUPLINK_CLIENT_ID, MYUPLINK_CLIENT_SECRET, MYUPLINK_DEVICE_ID
 *
 * <p>Run explicitly with:
 * mvn test -Dgroups=integration
 *
 * @author saki
 */
@Tag("integration")
class MyUplinkRestAdapterTestIT {

    private MyUplinkRestAdapter restAdapter;

    /**
     * Safe test temperature — same as current setpoint, so no real change occurs.
     */
    private static final double SAFE_TEST_TEMPERATURE = 48.0;

    @BeforeEach
    void setUp() {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.home")
                        + "/Entwicklung/SpringBoot/reference-project/enerflow/enerflow-app")
                .load();

        MyUplinkProperties properties = new MyUplinkProperties();
        properties.setBaseUrl("https://api.myuplink.com");
        properties.setClientId(dotenv.get("MYUPLINK_CLIENT_ID"));
        properties.setClientSecret(dotenv.get("MYUPLINK_CLIENT_SECRET"));
        properties.setDeviceId(dotenv.get("MYUPLINK_DEVICE_ID"));
        properties.setMinSetpointCelsius(30.0);
        properties.setMaxSetpointCelsius(65.0);
        properties.setTokenRefreshThresholdSeconds(60);

        MyUplinkTokenService tokenService = new MyUplinkTokenService(properties);
        restAdapter = new MyUplinkRestAdapter(properties, tokenService);
    }


    @Test
    void isAvailable_realApi_returnsTrue() {
        assertThat(restAdapter.isAvailable()).isTrue();
    }

    @Test
    void setHotWaterSetpoint_validTemperature_succeeds() {
        // Sets the setpoint to the current value — no actual change, just verifies the call works
        restAdapter.setHotWaterSetpoint(SAFE_TEST_TEMPERATURE);
    }

    @Test
    void setHotWaterSetpoint_belowMinimum_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> restAdapter.setHotWaterSetpoint(29.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

}