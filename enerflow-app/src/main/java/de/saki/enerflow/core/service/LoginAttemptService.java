// core/service/LoginAttemptService.java
package de.saki.enerflow.core.service;

import de.saki.enerflow.core.domain.AppUser;
import de.saki.enerflow.core.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Tracks failed login attempts per user and applies account lockout after
 * {@value #MAX_FAILED_ATTEMPTS} consecutive failures (US-04-01).
 *
 * <p>On successful login: resets the failure counter and records
 * {@code lastLogin}. On failed login: increments the counter and locks
 * the account for {@value #LOCKOUT_MINUTES} minutes once the threshold
 * is reached.
 *
 * @author saki
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_MINUTES = 15;

    private final AppUserRepository appUserRepository;

    public LoginAttemptService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Called after a successful login. Resets the failed attempt counter
     * and records the current timestamp as the last login.
     */
    public void onSuccessfulLogin(String username) {
        appUserRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLogin(LocalDateTime.now());
            appUserRepository.save(user);
        });
    }

    /**
     * Called after a failed login attempt. Increments the failure counter
     * and locks the account if the threshold is reached.
     * Does nothing if the username does not exist (prevents user enumeration).
     */
    public void onFailedLogin(String username) {
        Optional<AppUser> optionalUser = appUserRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            // Do not reveal whether the username exists or not
            return;
        }

        AppUser user = optionalUser.get();
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            log.warn("Account '{}' locked until {} after {} failed login attempts",
                    username, user.getLockedUntil(), attempts);
        }

        appUserRepository.save(user);
    }
}