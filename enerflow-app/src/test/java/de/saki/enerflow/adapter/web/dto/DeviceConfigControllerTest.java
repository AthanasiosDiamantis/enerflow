package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.config.SecurityConfig;
import de.saki.enerflow.config.security.JwtAuthenticationFilter;
import de.saki.enerflow.config.security.JwtService;
import de.saki.enerflow.core.service.DeviceConfigService;
import de.saki.enerflow.core.service.EnerFlowUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for {@link DeviceConfigController}.
 * The real {@link SecurityConfig} is imported so that {@code @PreAuthorize}
 * role checks are actually enforced during the test — this is the first
 * endpoint in the project protected by role (not just authentication),
 * so verifying rejection for the wrong role is the main point of this test.
 *
 * <p>Spring Security's servlet filter chain is disabled (addFilters = false):
 * {@code @PreAuthorize} is method-level AOP security, evaluated against
 * whatever Authentication {@code @WithMockUser} places in the
 * SecurityContext — it does not depend on the filter chain running. Leaving
 * filters enabled here would mean the mocked {@link JwtAuthenticationFilter}
 * (a no-op by default) never calls {@code filterChain.doFilter(...)},
 * silently blocking every request before it reaches the controller.
 *
 * @author saki
 */
@WebMvcTest(DeviceConfigController.class)
@Import(SecurityConfig.class)
class DeviceConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceConfigService deviceConfigService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EnerFlowUserDetailsService userDetailsService;

    private DeviceConfigDto validConfig() {
        return new DeviceConfigDto(300, 90, 55.0, 48.0, 300.0, 730);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET is allowed for MANAGER and returns the current configuration")
    void getConfig_allowedForManager() throws Exception {
        when(deviceConfigService.getAll()).thenReturn(validConfig());

        mockMvc.perform(get("/api/config/device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pvSurplusThresholdWatts").value(300))
                .andExpect(jsonPath("$.batterySocThresholdPercent").value(90));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET is allowed for ADMIN")
    void getConfig_allowedForAdmin() throws Exception {
        when(deviceConfigService.getAll()).thenReturn(validConfig());

        mockMvc.perform(get("/api/config/device"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET is rejected with 403 for plain USER role")
    void getConfig_rejectedForUser() throws Exception {
        mockMvc.perform(get("/api/config/device"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(deviceConfigService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("PATCH with valid values updates the config and returns the new state")
    void updateConfig_savesValidValues() throws Exception {
        DeviceConfigDto updated = new DeviceConfigDto(350, 85, 56.0, 47.0, 280.0, 730);
        when(deviceConfigService.getAll()).thenReturn(updated);

        mockMvc.perform(patch("/api/config/device")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pvSurplusThresholdWatts": 350,
                                  "batterySocThresholdPercent": 85,
                                  "hotwaterSetpointElevatedCelsius": 56.0,
                                  "hotwaterSetpointNormalCelsius": 47.0,
                                  "hotwaterTankVolumeLiters": 280.0,
                                  "snapshotRetentionDays": 730
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pvSurplusThresholdWatts").value(350));

        verify(deviceConfigService).updateConfig(any(DeviceConfigDto.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PATCH is rejected with 403 for plain USER role")
    void updateConfig_rejectedForUser() throws Exception {
        mockMvc.perform(patch("/api/config/device")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pvSurplusThresholdWatts": 350,
                                  "batterySocThresholdPercent": 85,
                                  "hotwaterSetpointElevatedCelsius": 56.0,
                                  "hotwaterSetpointNormalCelsius": 47.0,
                                  "hotwaterTankVolumeLiters": 280.0,
                                  "snapshotRetentionDays": 730
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(deviceConfigService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("PATCH rejects a non-positive PV surplus threshold with 400")
    void updateConfig_rejectsNonPositivePvThreshold() throws Exception {
        mockMvc.perform(patch("/api/config/device")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pvSurplusThresholdWatts": 0,
                                  "batterySocThresholdPercent": 90,
                                  "hotwaterSetpointElevatedCelsius": 55.0,
                                  "hotwaterSetpointNormalCelsius": 48.0,
                                  "hotwaterTankVolumeLiters": 300.0,
                                  "snapshotRetentionDays": 730
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(deviceConfigService, never()).updateConfig(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("PATCH rejects a battery SOC threshold outside 0-100 with 400")
    void updateConfig_rejectsOutOfRangeSoc() throws Exception {
        mockMvc.perform(patch("/api/config/device")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pvSurplusThresholdWatts": 300,
                                  "batterySocThresholdPercent": 150,
                                  "hotwaterSetpointElevatedCelsius": 55.0,
                                  "hotwaterSetpointNormalCelsius": 48.0,
                                  "hotwaterTankVolumeLiters": 300.0,
                                  "snapshotRetentionDays": 730
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(deviceConfigService, never()).updateConfig(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("PATCH rejects an elevated setpoint that is not higher than the normal setpoint")
    void updateConfig_rejectsElevatedNotHigherThanNormal() throws Exception {
        mockMvc.perform(patch("/api/config/device")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pvSurplusThresholdWatts": 300,
                                  "batterySocThresholdPercent": 90,
                                  "hotwaterSetpointElevatedCelsius": 48.0,
                                  "hotwaterSetpointNormalCelsius": 48.0,
                                  "hotwaterTankVolumeLiters": 300.0,
                                  "snapshotRetentionDays": 730
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(deviceConfigService, never()).updateConfig(any());
    }
}