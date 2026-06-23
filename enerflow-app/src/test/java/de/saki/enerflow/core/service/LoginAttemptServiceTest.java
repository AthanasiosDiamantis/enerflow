// test/.../core/service/LoginAttemptServiceTest.java
package de.saki.enerflow.core.service;

import de.saki.enerflow.core.domain.AppUser;
import de.saki.enerflow.core.domain.UserRole;
import de.saki.enerflow.core.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private LoginAttemptService service;

    @Test
    void onFailedLogin_incrementsFailedAttempts() {
        AppUser user = buildUser("saki", 0);
        when(appUserRepository.findByUsername("saki")).thenReturn(Optional.of(user));

        service.onFailedLogin("saki");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(1);
        assertThat(captor.getValue().getLockedUntil()).isNull();
    }

    @Test
    void onFailedLogin_locksAccountAfterMaxAttempts() {
        AppUser user = buildUser("saki", LoginAttemptService.MAX_FAILED_ATTEMPTS - 1);
        when(appUserRepository.findByUsername("saki")).thenReturn(Optional.of(user));

        service.onFailedLogin("saki");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getLockedUntil()).isNotNull();
        assertThat(captor.getValue().getLockedUntil())
                .isAfter(LocalDateTime.now().plusMinutes(14));
    }

    @Test
    void onFailedLogin_unknownUser_doesNothing() {
        when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        service.onFailedLogin("ghost");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void onSuccessfulLogin_resetsCounterAndSetsLastLogin() {
        AppUser user = buildUser("saki", 3);
        when(appUserRepository.findByUsername("saki")).thenReturn(Optional.of(user));

        service.onSuccessfulLogin("saki");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(0);
        assertThat(captor.getValue().getLastLogin()).isNotNull();
    }

    private AppUser buildUser(String username, int failedAttempts) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$hash");
        user.setRole(UserRole.ROLE_USER);
        user.setEnabled(true);
        user.setFailedLoginAttempts(failedAttempts);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}