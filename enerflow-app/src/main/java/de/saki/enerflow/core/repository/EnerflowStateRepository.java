package de.saki.enerflow.core.repository;

import de.saki.enerflow.core.domain.EnerflowState;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the singleton EnerFlow automation state.
 *
 * @author saki
 */
public interface EnerflowStateRepository extends JpaRepository<EnerflowState, Integer> {
}
