package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.config.SecurityConfig;
import de.saki.enerflow.config.security.JwtService;
import de.saki.enerflow.core.domain.UserRole;
import de.saki.enerflow.core.service.AppUserService;
import de.saki.enerflow.core.service.DuplicateUsernameException;
import de.saki.enerflow.core.service.EnerFlowUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for {@link AppUserController}.
 * The real {@link SecurityConfig} is imported so that {@code @PreAuthorize("hasRole('ADMIN')")}
 * is actually enforced during the test, following the same pattern established in
 * {@code DeviceConfigControllerTest}.
 *
 * @author saki
 */
@WebMvcTest(AppUserController.class)
@Import(SecurityConfig.class)
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EnerFlowUserDetailsService userDetailsService;

    private AppUserResponse sampleUser() {
        return new AppUserResponse(
                1L, "admin", UserRole.ROLE_ADMIN, true, false,
                LocalDateTime.of(2026, 7, 3, 12, 29),
                LocalDateTime.of(2026, 7, 9, 8, 25));
    }

    // ---- GET /api/admin/users ----

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET is allowed for ADMIN and returns the user list")
    void getAllUsers_allowedForAdmin() throws Exception {
        when(appUserService.getAll()).thenReturn(List.of(sampleUser()));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET is rejected with 403 for plain USER role")
    void getAllUsers_rejectedForUser() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appUserService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET is rejected with 403 for MANAGER role")
    void getAllUsers_rejectedForManager() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appUserService);
    }

    // ---- POST /api/admin/users ----

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST with valid values creates the user and returns 201")
    void createUser_savesValidValues() throws Exception {
        when(appUserService.createUser(any())).thenReturn(sampleUser());

        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "test1234",
                                  "role": "ROLE_USER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("admin"));

        verify(appUserService).createUser(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST is rejected with 403 for plain USER role")
    void createUser_rejectedForUser() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "test1234",
                                  "role": "ROLE_USER"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appUserService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("POST is rejected with 403 for MANAGER role")
    void createUser_rejectedForManager() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "test1234",
                                  "role": "ROLE_USER"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appUserService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST rejects a blank username with 400")
    void createUser_rejectsBlankUsername() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "",
                                  "password": "test1234",
                                  "role": "ROLE_USER"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(appUserService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST rejects a password shorter than 8 characters with 400")
    void createUser_rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "short",
                                  "role": "ROLE_USER"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(appUserService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST rejects a missing role with 400")
    void createUser_rejectsMissingRole() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "test1234"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(appUserService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST returns 409 when the username already exists")
    void createUser_rejectsDuplicateUsername() throws Exception {
        when(appUserService.createUser(any()))
                .thenThrow(new DuplicateUsernameException("testuser"));

        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "test1234",
                                  "role": "ROLE_USER"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists: testuser"));

        verify(appUserService).createUser(any());
    }
}