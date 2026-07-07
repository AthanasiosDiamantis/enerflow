package de.saki.enerflow.config.security;

import de.saki.enerflow.core.service.EnerFlowUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private EnerFlowUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("passes the request through without authenticating when no Authorization header is present")
    void doFilterInternal_passesThrough_whenNoAuthorizationHeader() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("passes the request through without authenticating when the header lacks the Bearer prefix")
    void doFilterInternal_passesThrough_whenHeaderMissingBearerPrefix() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("authenticates the user in the SecurityContext when the token is valid")
    void doFilterInternal_authenticatesUser_whenTokenIsValid() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        UserDetails userDetails = new User("saki", "irrelevant", Collections.emptyList());

        when(jwtService.extractUsername("valid-token")).thenReturn("saki");
        when(userDetailsService.loadUserByUsername("saki")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("does not authenticate when a valid token belongs to a user that fails validation")
    void doFilterInternal_doesNotAuthenticate_whenTokenValidationFails() throws Exception {
        request.addHeader("Authorization", "Bearer some-token");
        UserDetails userDetails = new User("saki", "irrelevant", Collections.emptyList());

        when(jwtService.extractUsername("some-token")).thenReturn("saki");
        when(userDetailsService.loadUserByUsername("saki")).thenReturn(userDetails);
        when(jwtService.isTokenValid("some-token", userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("does not authenticate and still continues the chain when the JWT is invalid or expired")
    void doFilterInternal_continuesChain_whenJwtServiceThrows() throws Exception {
        request.addHeader("Authorization", "Bearer broken-token");

        when(jwtService.extractUsername("broken-token"))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("bad token"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("skips re-authentication when the SecurityContext already holds an authentication")
    void doFilterInternal_skipsReauthentication_whenAlreadyAuthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");

        UserDetails existingUser = new User("already-logged-in", "irrelevant", Collections.emptyList());
        Authentication existingAuth = new UsernamePasswordAuthenticationToken(
                existingUser, null, existingUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.extractUsername("valid-token")).thenReturn("saki");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }
}