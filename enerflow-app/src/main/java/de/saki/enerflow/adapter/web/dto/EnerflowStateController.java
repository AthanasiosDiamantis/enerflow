package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.core.domain.EnerflowState;
import de.saki.enerflow.core.repository.EnerflowStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST endpoint exposing the EnerFlow automation state.
 * Allows querying the current state and toggling automation on/off.
 *
 * <p>No access control is applied yet — role-based security is planned
 * as a separate work package once a real user model exists.
 *
 * @author saki
 */
@RestController
@RequestMapping("/api/enerflow")
public class EnerflowStateController {

    private static final Logger log = LoggerFactory.getLogger(EnerflowStateController.class);

    private final EnerflowStateRepository stateRepository;

    public EnerflowStateController(EnerflowStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    /**
     * Returns the current EnerFlow automation state.
     */
    @GetMapping("/status")
    public EnerflowStateDto getState() {
        return EnerflowStateDto.fromEntity(loadState());
    }

    /**
     * Toggles EnerFlow automation on/off. Accessible to all authenticated users
     * (ROLE_USER, ROLE_MANAGER, ROLE_ADMIN) — per US-04-02, the Hausbesitzer
     * (ROLE_USER) must be able to activate/deactivate EnerFlow.
     * Role-restricted endpoints (e.g. device configuration) follow in Sprint 4.
     */
    @PatchMapping("/toggle")
    public EnerflowStateDto toggle() {
        EnerflowState state = loadState();

        boolean newEnabledValue = !state.isEnabled();
        state.setEnabled(newEnabledValue);
        state.setLastUpdated(LocalDateTime.now());
        stateRepository.save(state);

        log.info("EnerFlow automation is now {}", newEnabledValue ? "enabled" : "disabled");

        return EnerflowStateDto.fromEntity(state);
    }

    private EnerflowState loadState() {
        return stateRepository.findById(1)
                .orElseThrow(() -> new IllegalStateException(
                        "enerflow_state row with id=1 not found — check V3 migration"));
    }


}
