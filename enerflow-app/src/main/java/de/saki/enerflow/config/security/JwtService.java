package de.saki.enerflow.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and validates JSON Web Tokens (JWTs) for EnerFlow's stateless
 * authentication. Tokens are signed with HMAC-SHA256 (HS256) using a
 * shared secret key configured via {@code enerflow.security.jwt-secret}.
 *
 * <p>A JWT consists of three Base64-encoded parts separated by dots:
 * header (algorithm), payload (claims: username, issued-at, expiry),
 * and signature. The signature is what makes it tamper-proof.
 *
 * @author saki
 */
@Service
public class JwtService {

    @Value("${enerflow.security.jwt-secret}")
    private String jwtSecret;

    @Value("${enerflow.security.jwt-expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Generates a signed JWT for the given user.
     * The subject claim contains the username; the token is valid for
     * {@code enerflow.security.jwt-expiration-ms} milliseconds.
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username (subject claim) from a JWT.
     * Throws a JwtException if the token is invalid or expired.
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Returns true if the token is valid and belongs to the given user.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
