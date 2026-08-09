package com.roamate.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Minimal DEV-ONLY auth flow: issues an HS256 JWT for a given userId with
 * no password/credential check, purely so the rest of the app (which
 * expects a valid Bearer token on every request per SecurityConfig) has
 * something to call. This is intentionally NOT production auth - swap in
 * a real identity provider (Cognito, Auth0, your own credential store)
 * before shipping. Kept here so the "no auth flow" gap from the spec
 * audit isn't silently left as dead configuration.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SecretKey signingKey;

    public AuthController(@Value("${roamate.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public record DevLoginRequest(String userId) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}

    @PostMapping("/dev-login")
    public TokenResponse devLogin(@RequestBody DevLoginRequest request) {
        Instant now = Instant.now();
        Instant expiry = now.plus(24, ChronoUnit.HOURS);

        // Explicitly HS256, not left to jjwt's Keys.hmacShaKeyFor() length-based
        // auto-selection (which picks HS384 for keys >=384 bits, e.g. the
        // default 60-byte/480-bit dev secret). SecurityConfig's
        // NimbusJwtDecoder.withSecretKey() defaults to HS256-only validation
        // regardless of key length, so a mismatch here means every issued
        // token fails signature validation on every protected endpoint -
        // silently, since /dev-login itself is unprotected and returns 200
        // either way.
        String token = Jwts.builder()
                .subject(request.userId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new TokenResponse(token, "Bearer", ChronoUnit.SECONDS.between(now, expiry));
    }

    /**
     * Simple health check / test endpoint.
     * Accessible via GET /api/v1/auth/hello
     */
    @GetMapping("/hello")
    public String sayHello() {
        return "hello";
    }
}