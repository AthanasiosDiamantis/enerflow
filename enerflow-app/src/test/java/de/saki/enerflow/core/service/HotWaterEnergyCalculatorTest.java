// test/java/de/saki/enerflow/core/service/HotWaterEnergyCalculatorTest.java
package de.saki.enerflow.core.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class HotWaterEnergyCalculatorTest {

    @Test
    void calculateKwh_typicalBoost_returnsExpectedValue() {
        // 300 liters, raised by 7 K (48°C -> 55°C)
        double kwh = HotWaterEnergyCalculator.calculateKwh(300, 7);

        assertThat(kwh).isCloseTo(2.44, offset(0.01));
    }

    @Test
    void calculateKwh_negativeDelta_returnsNegativeValue() {
        // cooling down (RESET) should yield a negative kWh value
        double kwh = HotWaterEnergyCalculator.calculateKwh(300, -7);

        assertThat(kwh).isNegative();
    }
}