package de.saki.enerflow.core.model;

/**
 * Represents a heat-generating device whose target temperature can be controlled.
 * (e.g. heat pump for domestic hot water).
 */
public interface HeatGenerator {

    /**
     *
     * @return the current actual water temparature in degrees Celsius
     */
    double getCurrentTemperatureCelsius();

    /**
     *
     * @return the current target (setpoint) temperature in degrees Celsius
     */
    double getSetpointTemperatureCelsius();

    /**
     *
     * @param targetTemperatureCelsius the desired setpoint temperature in degrees Celsius
     * @throws IllegalArgumentException if the value is outside the allowed safety range of temperature
     */
    void setSetpointTemperatureCelsius(double targetTemperatureCelsius);

    /**
     *
     * @return true it the device is reachable and delivering valid data.
     */
    boolean isAvailable();
}
