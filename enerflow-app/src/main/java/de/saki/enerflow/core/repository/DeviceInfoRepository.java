// core/repository/DeviceInfoRepository.java
package de.saki.enerflow.core.repository;

import de.saki.enerflow.core.domain.DeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceInfoRepository extends JpaRepository<DeviceInfo, String> {
}