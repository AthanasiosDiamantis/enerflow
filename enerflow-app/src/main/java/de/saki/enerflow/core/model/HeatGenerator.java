package de.saki.enerflow.core.model;

/**
 * Represents a heat-generating device whose target temperature can be controlled.
 * (e.g. heat pump for domestic hot water).
 */
public interface HeatGenerator {

    /**
     * @return the current actual hot water temperature in degrees Celsius
     */
    double getHotWaterTemperatureCelsius();

    /**
     * @return the current hot water setpoint temperature in degrees Celsius
     */
    double getHotWaterSetpointCelsius();

    /**
     * @param targetTemperatureCelsius the desired setpoint temperature in degrees Celsius
     * @throws IllegalArgumentException if the value is outside the allowed safety range
     */
    void setHotWaterSetpoint(double targetTemperatureCelsius);

    /**
     * @return true if the device is reachable and delivering valid data
     */
    boolean isAvailable();
}
