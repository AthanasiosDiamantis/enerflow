package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.config.security.JwtAuthenticationFilter;
import de.saki.enerflow.core.domain.EnerflowState;
import de.saki.enerflow.core.repository.EnerflowStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for {@link EnerflowStateController}.
 * Spring Security's filter chain is disabled here (addFilters = false) —
 * this test focuses on controller/repository interaction, not authentication.
 * JWT-specific behaviour is covered separately in the JWT test classes.
 *
 * @author saki
 */
@WebMvcTest(EnerflowStateController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnerflowStateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnerflowStateRepository stateRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private EnerflowState buildState(boolean enabled, boolean boostActive) {
        EnerflowState state = new EnerflowState();
        state.setId(1);
        state.setEnabled(enabled);
        state.setBoostActive(boostActive);
        state.setLastKnownSetpoint(47.0);
        state.setLastUpdated(LocalDateTime.now().minusMinutes(10));
        return state;
    }

    @Test
    @DisplayName("GET /api/enerflow/status returns the current state as JSON")
    void getState_returnsCurrentState() throws Exception {
        EnerflowState state = buildState(true, false);
        when(stateRepository.findById(1)).thenReturn(Optional.of(state));

        mockMvc.perform(get("/api/enerflow/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.boostActive").value(false))
                .andExpect(jsonPath("$.lastKnownSetpoint").value(47.0));
    }

    @Test
    @DisplayName("GET /api/enerflow/status propagates an exception when the state singleton is missing")
    void getState_throwsException_whenStateMissing() {
        when(stateRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mockMvc.perform(get("/api/enerflow/status")))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("enerflow_state row with id=1 not found — check V3 migration");
    }

    @Test
    @DisplayName("PATCH /api/enerflow/toggle flips enabled from false to true")
    void toggle_flipsEnabled_fromFalseToTrue() throws Exception {
        EnerflowState state = buildState(false, false);
        when(stateRepository.findById(1)).thenReturn(Optional.of(state));
        when(stateRepository.save(any(EnerflowState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/enerflow/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(stateRepository).save(state);
        assertThat(state.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("PATCH /api/enerflow/toggle flips enabled from true to false")
    void toggle_flipsEnabled_fromTrueToFalse() throws Exception {
        EnerflowState state = buildState(true, false);
        when(stateRepository.findById(1)).thenReturn(Optional.of(state));
        when(stateRepository.save(any(EnerflowState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/enerflow/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(stateRepository).save(state);
        assertThat(state.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("PATCH /api/enerflow/toggle updates lastUpdated to the current time")
    void toggle_updatesLastUpdatedTimestamp() throws Exception {
        LocalDateTime before = LocalDateTime.now().minusDays(1);
        EnerflowState state = buildState(false, false);
        state.setLastUpdated(before);
        when(stateRepository.findById(1)).thenReturn(Optional.of(state));
        when(stateRepository.save(any(EnerflowState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<EnerflowState> captor = ArgumentCaptor.forClass(EnerflowState.class);

        mockMvc.perform(patch("/api/enerflow/toggle"))
                .andExpect(status().isOk());

        verify(stateRepository).save(captor.capture());
        assertThat(captor.getValue().getLastUpdated()).isAfter(before);
    }

}