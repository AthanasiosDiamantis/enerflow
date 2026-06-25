package de.saki.enerflow.core.repository;

import de.saki.enerflow.core.domain.ElectricityPriceChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the electricity price change log (append-only).
 *
 * @author saki
 */
@Repository
public interface ElectricityPriceChangeLogRepository
        extends JpaRepository<ElectricityPriceChangeLog, Long> {
}