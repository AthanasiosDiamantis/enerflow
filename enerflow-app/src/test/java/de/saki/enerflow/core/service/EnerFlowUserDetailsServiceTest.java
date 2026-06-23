// test/.../core/service/EnerFlowUserDetailsServiceTest.java
package de.saki.enerflow.core.service;

import de.saki.enerflow.core.domain.AppUser;
import de.saki.enerflow.core.domain.UserRole;
import de.saki.enerflow.core.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnerFlowUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private EnerFlowUserDetailsService service;

    @Test
    void loadUserByUsername_existingUser_returnsCorrectUserDetails() {
        AppUser user = buildUser("admin", UserRole.ROLE_ADMIN, true, null);
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFoundException() {
        when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_lockedAccount_returnsLockedUserDetails() {
        AppUser user = buildUser("saki", UserRole.ROLE_USER, true,
                LocalDateTime.now().plusMinutes(10));
        when(appUserRepository.findByUsername("saki")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("saki");

        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_expiredLock_returnsNonLockedUserDetails() {
        AppUser user = buildUser("saki", UserRole.ROLE_USER, true,
                LocalDateTime.now().minusMinutes(1));
        when(appUserRepository.findByUsername("saki")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("saki");

        // Lock has expired -> account should be accessible again
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    private AppUser buildUser(String username, UserRole role,
                              boolean enabled, LocalDateTime lockedUntil) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setRole(role);
        user.setEnabled(enabled);
        user.setLockedUntil(lockedUntil);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}