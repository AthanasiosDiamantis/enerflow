package de.saki.enerflow.adapter.heatpump.myuplink;

import de.saki.enerflow.config.MyUplinkProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * Unit test for {@link MyUplinkRestAdapter}.
 * The myUplink REST API and token service are mocked, no real HTTP calls are made.
 *
 * @author saki
 */
@ExtendWith(MockitoExtension.class)
class MyUplinkRestAdapterTest {

    @Mock
    private MyUplinkProperties properties;

    @Mock
    private MyUplinkTokenService tokenService;

    @InjectMocks
    private MyUplinkRestAdapter restAdapter;

    @BeforeEach
    void setUp() {
        // Define the safety range for setpoints in all tests
        Mockito.when(properties.getMinSetpointCelsius()).thenReturn(30.0);
        Mockito.when(properties.getMaxSetpointCelsius()).thenReturn(65.0);
    }

    // -------------------------------------------------------------------------
    // Validation tests
    // -------------------------------------------------------------------------


    @Test
    @DisplayName("setHotWaterSetpoint: rejects temperature below minimum")
    void setHotWaterSetpoint_belowMinimum_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> restAdapter.setHotWaterSetpoint(29.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Setpoint 29,0 °C is outside the allowed range [30,0 – 65,0 °C]");
    }

}