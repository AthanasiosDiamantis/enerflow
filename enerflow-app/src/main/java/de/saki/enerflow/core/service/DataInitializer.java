package de.saki.enerflow.core.service;

import de.saki.enerflow.core.domain.AppUser;
import de.saki.enerflow.core.domain.UserRole;
import de.saki.enerflow.core.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds a default admin user on application startup if no users exist yet.
 * The default password is read from application.yaml
 * ({@code enerflow.security.default-admin-password}) and BCrypt-hashed
 * before persistence — plaintext is never stored.
 *
 * <p>This component only runs when no users are present in the database,
 * so it is safe to leave active in production (it won't overwrite an
 * existing admin account).
 *
 * @author saki
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    // In DataInitializer.java — nur diese Zeile ändern:
    @Value("${enerflow.security.default-admin-password:admin123}")
    private String defaultAdminPassword;

    public DataInitializer(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    AppUser admin = new AppUser();

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            log.debug("Users already exist — skipping default admin creation");
            return;
        }

        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(defaultAdminPassword));
        admin.setRole(UserRole.ROLE_ADMIN);
        admin.setEnabled(true);
        admin.setFailedLoginAttempts(0);
        admin.setCreatedAt(LocalDateTime.now());

        appUserRepository.save(admin);

        log.info("Default admin user created (username: 'admin'). " +
                "Change the password before going to production!");
    }

}