package de.saki.enerflow.core.repository;

import de.saki.enerflow.core.domain.BatterySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatterySnapshotRepository extends JpaRepository<BatterySnapshot, Long> {
}
