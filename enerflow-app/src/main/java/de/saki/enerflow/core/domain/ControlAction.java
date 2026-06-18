package de.saki.enerflow.core.domain;

/**
 * Type of control action recorded in {@link ControlLog}.
 * RAISE/RESET are performed automatically by EnergyManagerService,
 * MANUAL is a direct user override (relevant once AP3.3 is in place).
 *
 * @author saki
 */
public enum ControlAction {
    RAISE,
    RESET,
    MANUAL
}
