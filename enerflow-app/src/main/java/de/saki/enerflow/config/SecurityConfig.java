package de.saki.enerflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Minimal security configuration for EnerFlow's REST API.
 *
 * <p>Enables HTTP Basic authentication and disables CSRF protection for
 * {@code /api/**} endpoints. CSRF protection is designed for browser
 * session-based web applications; it is not applicable to stateless
 * REST APIs accessed via Basic Auth or tokens.
 *
 * <p>Note: this is intentionally minimal. Role-based access control
 * (e.g. restricting /api/enerflow/toggle to specific roles) is planned
 * as a separate, dedicated work package once a real user model exists.
 * Currently, all authenticated users have full access to all endpoints.
 *
 * @author saki
 */
@Configuration
public class SecurityConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}