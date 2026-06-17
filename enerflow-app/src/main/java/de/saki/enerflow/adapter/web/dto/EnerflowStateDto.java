package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.core.domain.EnerflowState;

import java.time.LocalDateTime;

/**
 * Data transfer object for exposing {@link EnerflowState} via REST.
 * Decouples the API contract from the JPA entity structure.
 *
 * @author saki
 */
public record EnerflowStateDto(
        boolean enabled,
        boolean boostActive,
        Double lastKnownSetpoint,
        LocalDateTime lastUpdated
) {
    public static EnerflowStateDto fromEntity(EnerflowState state) {
        return new EnerflowStateDto(
                state.isEnabled(),
                state.isBoostActive(),
                state.getLastKnownSetpoint(),
                state.getLastUpdated()
        );
    }
}
