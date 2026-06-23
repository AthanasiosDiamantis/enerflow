// core/service/EnerFlowUserDetailsService.java
package de.saki.enerflow.core.service;

import de.saki.enerflow.core.domain.AppUser;
import de.saki.enerflow.core.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Loads EnerFlow users from the database for Spring Security's authentication
 * process. This is the bridge between our {@link AppUser} entity and Spring
 * Security's internal {@link UserDetails} representation.
 *
 * <p>Also checks account lockout: if {@code lockedUntil} is set and still
 * in the future, the returned {@code UserDetails} marks the account as locked.
 * Spring Security will then refuse the login automatically with a 401.
 *
 * @author saki
 */
@Service
public class EnerFlowUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public EnerFlowUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + username));

        boolean accountNonLocked = user.getLockedUntil() == null
                || user.getLockedUntil().isBefore(LocalDateTime.now());

        // Note: we use authorities() with the full role name (e.g. "ROLE_ADMIN"),
        // not roles() which would add a "ROLE_" prefix automatically — our enum
        // already contains the prefix, so roles() would produce "ROLE_ROLE_ADMIN".
        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().name())))
                .accountLocked(!accountNonLocked)
                .disabled(!user.isEnabled())
                .build();
    }
}