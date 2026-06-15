package de.saki.enerflow.core.repository;

import de.saki.enerflow.core.domain.DeviceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for accessing runtime-configurable EnerFlow parameters.
 *
 * @author saki
 */
public interface DeviceConfigRepository extends JpaRepository<DeviceConfig, String> {
}
