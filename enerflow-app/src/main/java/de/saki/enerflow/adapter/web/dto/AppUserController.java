package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.core.service.AppUserService;
import de.saki.enerflow.core.service.DuplicateUsernameException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoint for ADMIN-level user management (US-04-02).
 * Allows listing existing accounts and creating new ones. Restricted to ROLE_ADMIN.
 *
 * @author saki
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppUserResponse>> getAllUsers() {
        return ResponseEntity.ok(appUserService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        AppUserResponse created = appUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUsername(DuplicateUsernameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}