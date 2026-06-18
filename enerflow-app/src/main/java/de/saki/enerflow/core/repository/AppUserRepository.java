// core/repository/AppUserRepository.java
package de.saki.enerflow.core.repository;

import de.saki.enerflow.core.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}