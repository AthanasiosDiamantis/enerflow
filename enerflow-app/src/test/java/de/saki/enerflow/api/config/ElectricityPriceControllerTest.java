package de.saki.enerflow.api.config;

import de.saki.enerflow.config.SecurityConfig;
import de.saki.enerflow.config.security.JwtAuthenticationFilter;
import de.saki.enerflow.core.domain.ElectricityPriceChangeLog;
import de.saki.enerflow.core.domain.ElectricityPriceConfig;
import de.saki.enerflow.core.repository.ElectricityPriceChangeLogRepository;
import de.saki.enerflow.core.repository.ElectricityPriceRepository;
import de.saki.enerflow.core.service.EnerFlowUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for {@link ElectricityPriceController}.
 * The real {@link SecurityConfig} is imported explicitly so that the
 * {@code @AuthenticationPrincipal} argument resolver is reliably registered
 * (relying on Spring Boot's generic security fallback instead proved
 * unreliable for this resolver). Spring Security's filter chain itself is
 * still disabled (addFilters = false) — we only need the resolver, not
 * actual request-time authorization enforcement.
 *
 * @author saki
 */
@WebMvcTest(ElectricityPriceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@WithMockUser(username = "saki")
class ElectricityPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ElectricityPriceRepository repository;

    @MockitoBean
    private ElectricityPriceChangeLogRepository changeLogRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // Required because SecurityConfig's constructor needs it, even though
    // no test here exercises real authentication against it.
    @MockitoBean
    private EnerFlowUserDetailsService userDetailsService;

    private ElectricityPriceConfig buildConfig(double price) {
        ElectricityPriceConfig config = new ElectricityPriceConfig();
        config.setId(1);
        config.setPriceCentPerKwh(price);
        config.setUpdatedAt(LocalDateTime.now().minusDays(1));
        config.setUpdatedBy("admin");
        return config;
    }

    @Test
    @DisplayName("GET /api/config/electricity-price returns the current price")
    void getPrice_returnsCurrentPrice() throws Exception {
        when(repository.findById(1)).thenReturn(Optional.of(buildConfig(28.0)));

        mockMvc.perform(get("/api/config/electricity-price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCentPerKwh").value(28.0));
    }

    @Test
    @DisplayName("GET /api/config/electricity-price returns 404 when no price is configured")
    void getPrice_returns404_whenNotConfigured() throws Exception {
        when(repository.findById(1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/config/electricity-price"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH updates the price, writes an audit log entry, and returns the updated config")
    void updatePrice_updatesPriceAndWritesAuditLog() throws Exception {
        ElectricityPriceConfig existing = buildConfig(28.0);
        when(repository.findById(1)).thenReturn(Optional.of(existing));
        when(repository.save(any(ElectricityPriceConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/config/electricity-price")
                        .param("price", "32.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCentPerKwh").value(32.5))
                .andExpect(jsonPath("$.updatedBy").value("saki"));

        ArgumentCaptor<ElectricityPriceChangeLog> logCaptor =
                ArgumentCaptor.forClass(ElectricityPriceChangeLog.class);
        verify(changeLogRepository).save(logCaptor.capture());

        assertThat(logCaptor.getValue().getOldPrice()).isEqualTo(28.0);
        assertThat(logCaptor.getValue().getNewPrice()).isEqualTo(32.5);
        assertThat(logCaptor.getValue().getChangedBy()).isEqualTo("saki");
    }

    @Test
    @DisplayName("PATCH rejects a price of zero with 400 Bad Request and writes no audit log")
    void updatePrice_rejectsZeroPrice() throws Exception {
        mockMvc.perform(patch("/api/config/electricity-price")
                        .param("price", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(changeLogRepository);
    }

    @Test
    @DisplayName("PATCH rejects a negative price with 400 Bad Request and writes no audit log")
    void updatePrice_rejectsNegativePrice() throws Exception {
        mockMvc.perform(patch("/api/config/electricity-price")
                        .param("price", "-5.0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(changeLogRepository);
    }

    @Test
    @DisplayName("PATCH propagates an exception when the price singleton is missing")
    void updatePrice_throwsException_whenConfigMissing() {
        when(repository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mockMvc.perform(patch("/api/config/electricity-price")
                .param("price", "30.0")))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Electricity price singleton missing in database");

        verifyNoInteractions(changeLogRepository);
    }
}