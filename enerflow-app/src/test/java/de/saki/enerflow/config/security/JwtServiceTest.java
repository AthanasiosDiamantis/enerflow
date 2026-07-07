package de.saki.enerflow.config.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 * No Spring context is loaded — the @Value-injected fields (jwtSecret,
 * jwtExpirationMs) are set manually via ReflectionTestUtils, since there
 * are no setters for them in production code.
 *
 * @author saki
 */
class JwtServiceTest {

    // Exactly 32 bytes = 256 bits — the minimum for HS256. Both secrets below
    // are deliberately the same length so jjwt selects the same algorithm
    // (HS256) for both; otherwise a mismatched key length can trigger
    // WeakKeyException instead of the SignatureException we actually want
    // to test here.
    private static final String TEST_SECRET = "enerflow-test-secret-key-256bit!";

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L); // 1 hour

        userDetails = new User("saki", "irrelevant-password", Collections.emptyList());
    }

    @Test
    @DisplayName("generates a token with the standard three-part JWT structure")
    void generateToken_producesThreePartToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extracts the correct username from a freshly generated token")
    void extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("saki");
    }

    @Test
    @DisplayName("marks a freshly generated token as valid for the matching user")
    void isTokenValid_returnsTrue_forMatchingUserAndFreshToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("marks a token as invalid when the username does not match")
    void isTokenValid_returnsFalse_whenUsernameMismatches() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = new User("someone-else", "pw", Collections.emptyList());

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("throws when parsing an already-expired token")
    void extractUsername_throws_whenTokenIsExpired() {
        // Negative expiration means the token's exp claim is already in the past
        // the instant it's created.
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);
        String expiredToken = jwtService.generateToken(userDetails);

        // jjwt validates the exp claim during parsing itself — it throws
        // ExpiredJwtException before our own isTokenExpired() check is ever reached.
        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("throws when the token is malformed")
    void extractUsername_throws_forMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-valid-jwt-token"))
                .isInstanceOf(io.jsonwebtoken.MalformedJwtException.class);
    }

    @Test
    @DisplayName("throws when the token was signed with a different secret")
    void extractUsername_throws_whenSignedWithDifferentSecret() {
        String token = jwtService.generateToken(userDetails);

        // Simulate a token forged with (or generated under) a different secret
        // by switching the service's secret after the token was created.
        // Same length as TEST_SECRET (32 bytes) so both use HS256 — only the
        // signature itself differs, giving us a clean SignatureException.
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "enerflow-different-secret-key-32");

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(SignatureException.class);
    }
}