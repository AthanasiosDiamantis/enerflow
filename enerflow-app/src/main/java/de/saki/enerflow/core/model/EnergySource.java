package de.saki.enerflow.core.model;

/**
 * Represents any source of electrical energy (e.g. photovoltaic system).
 * Implementations provide current production and surplus data.
 *
 * @author saki
 */
public interface EnergySource {

    /**
     * @return the current production in watts
     */
    int getProductionWatts();

    /**
     * Returns the current surplus power available for redistribution in watts.
     * Surplus = production - consumption - battery charging
     *
     * @return  returns the surplus electricity power for redistribution in watts
     */
    int getSurplusWatts();

    /**
     * @return true if this energy source is reachable and delivering valid data.
     */
    boolean isAvailable();
}
