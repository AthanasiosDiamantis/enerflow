package de.saki.enerflow.core.service;

import de.saki.enerflow.adapter.web.dto.AppUserResponse;
import de.saki.enerflow.adapter.web.dto.CreateUserRequest;
import de.saki.enerflow.core.domain.AppUser;
import de.saki.enerflow.core.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Application service for ADMIN-level user management (US-04-02).
 * Handles listing existing users and creating new accounts. Passwords are
 * hashed with BCrypt before persisting; plaintext never reaches the repository.
 *
 * @author saki
 */
@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public List<AppUserResponse> getAll() {
        return appUserRepository.findAll().stream()
                .map(AppUserResponse::fromEntity)
                .toList();
    }

    public AppUserResponse createUser(CreateUserRequest request) {
        if (appUserRepository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateUsernameException(request.username());
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());

        AppUser saved = appUserRepository.save(user);
        return AppUserResponse.fromEntity(saved);
    }
}