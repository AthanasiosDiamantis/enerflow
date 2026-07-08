// AuthController.java — vollständig ersetzen:
package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.config.security.JwtService;
import de.saki.enerflow.core.service.EnerFlowUserDetailsService;
import de.saki.enerflow.core.service.LoginAttemptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles authentication requests. On successful login, returns a signed JWT
 * that the client must include in subsequent requests as:
 * {@code Authorization: Bearer <token>}
 *
 * <p>Failed login attempts are tracked per user (AP3.3 Chunk 3):
 * after {@value LoginAttemptService#MAX_FAILED_ATTEMPTS} consecutive failures
 * the account is locked for {@value LoginAttemptService#LOCKOUT_MINUTES}
 * minutes.
 *
 * @author saki
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final EnerFlowUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager,
                          EnerFlowUserDetailsService userDetailsService,
                          JwtService jwtService,
                          LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(), request.password()));

            loginAttemptService.onSuccessfulLogin(request.username());

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
            String token = jwtService.generateToken(userDetails);
            String role = userDetails.getAuthorities().iterator().next().getAuthority();

            return ResponseEntity.ok(new LoginResponse(token, role));

        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Account is locked. Try again later."));

        } catch (BadCredentialsException e) {
            loginAttemptService.onFailedLogin(request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid username or password."));
        }
    }

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token, String role) {}
    public record ErrorResponse(String message) {}
}