// core/service/HotWaterEnergyCalculator.java
package de.saki.enerflow.core.service;

/**
 * Pure utility for calculating the thermal energy (in kWh) required to heat
 * a given volume of water by a given temperature difference.
 *
 * <p>Formula: Q = m * c * deltaT, with m in kg (1 liter water = 1 kg) and
 * c = 4186 J/(kg*K) (specific heat capacity of water). Converted from joules
 * to kWh by dividing by 3,600,000 J/kWh -> 4186 / 3,600,000 ≈ 0.001163.
 *
 * <p>Note: this is the theoretical thermal energy stored in the water, not
 * the heat pump's electrical energy consumption (which depends on its COP
 * and is not modeled here).
 *
 * @author saki
 */
public final class HotWaterEnergyCalculator {

    private static final double SPECIFIC_HEAT_KWH_PER_LITER_KELVIN = 0.001163;

    private HotWaterEnergyCalculator() {
        // utility class — no instances
    }

    public static double calculateKwh(double volumeLiters, double deltaTemperatureCelsius) {
        return volumeLiters * SPECIFIC_HEAT_KWH_PER_LITER_KELVIN * deltaTemperatureCelsius;
    }
}