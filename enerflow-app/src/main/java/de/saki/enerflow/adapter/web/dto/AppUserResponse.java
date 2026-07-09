// ====TRENNER====
// adapter/web/dto/AppUserResponse.java
package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.core.domain.AppUser;
import de.saki.enerflow.core.domain.UserRole;

import java.time.LocalDateTime;

/**
 * Response payload representing an application user, safe for API exposure.
 * Never includes the password hash or failed-login counter.
 *
 * @author saki
 */
public record AppUserResponse(
        Long id,
        String username,
        UserRole role,
        boolean enabled,
        boolean locked,
        LocalDateTime createdAt,
        LocalDateTime lastLogin
) {

    public static AppUserResponse fromEntity(AppUser user) {
        boolean isLocked = user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now());

        return new AppUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isEnabled(),
                isLocked,
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }
}