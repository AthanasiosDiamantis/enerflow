package de.saki.enerflow.core.repository;

import de.saki.enerflow.core.domain.ElectricityPriceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the electricity price singleton (id = 1).
 *
 * @author saki
 */
@Repository
public interface ElectricityPriceRepository
        extends JpaRepository<ElectricityPriceConfig, Integer> {
}