package de.saki.enerflow.core.repository;

import de.saki.enerflow.core.domain.HeatpumpSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for storing and retrieving heat pump snapshots.
 * Spring Data JPA generates all queries automatically.
 */
@Repository
public interface HeatpumpSnapshotRepository extends JpaRepository<HeatpumpSnapshot, Long> {

    long deleteByTimestampBefore(LocalDateTime threshold);

    List<HeatpumpSnapshot> findByTimestampBetween(
            LocalDateTime from,
            LocalDateTime to);

    Optional<HeatpumpSnapshot> findTopByOrderByTimestampDesc();
}
