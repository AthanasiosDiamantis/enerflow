package de.saki.enerflow.adapter.battery.sonnen;

import de.saki.enerflow.config.SonnenBatteryProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link SonnenBatteryAdapter}.
 * Makes real HTTP calls to the local sonnenBatterie REST API.
 *
 * <p>Requires valid credentials in the .env file:
 * SONNEN_HOST, SONNEN_API_TOKEN
 *
 * <p>Run explicitly with:
 * mvn test -Dgroups=integration -DexcludedGroups=
 *
 * @author saki
 */
@Tag("integration")
class SonnenBatteryAdapterTestIT {

    private SonnenBatteryAdapter adapter;

    @BeforeEach
    void setUp() {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.home") + "/Entwicklung/SpringBoot/reference-project/enerflow/enerflow-app")
                .load();

        SonnenBatteryProperties properties = new SonnenBatteryProperties();
        properties.setHost(dotenv.get("SONNEN_HOST"));
        properties.setApiToken(dotenv.get("SONNEN_API_TOKEN"));

        adapter = new SonnenBatteryAdapter(properties);
    }

    @Test
    void isAvailable_realApi_returnsTrue() {
        assertThat(adapter.isAvailable()).isTrue();
    }

    @Test
    void getStateOfChargePercent_returnsValueInValidRange() {
        int stateOfChargePercent = adapter.getStateOfChargePercent();
        assertThat(stateOfChargePercent).isBetween(0, 100);
    }

    @Test
    void getProductionWatts_returnsNonNegativeValue() {
        assertThat(adapter.getProductionWatts()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getSurplusWatts_returnsPlausibleValue() {
        // Surplus can be negative (grid import) or positive (grid feed-in)
        // Just verify the call succeeds and returns a plausible range
        int surplus = adapter.getSurplusWatts();
        assertThat(surplus).isBetween(-20000, 20000);
    }

    @Test
    void isCharging_and_isDischarging_areNotBothTrue() {
        // A battary cannot charge and discharge at the same time
        assertThat(adapter.isCharging() && adapter.isDischarging()).isFalse();
    }

}