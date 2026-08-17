package com.roamate.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
 *
 * /register and /login below are the real replacement: a username +
 * password account backed by UserService/User, so a given person keeps
 * one stable identity (and their trip history) across reinstalls and
 * devices instead of getting a fresh random device id every time.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SecretKey signingKey;
    private final UserService userService;

    public AuthController(@Value("${roamate.jwt.secret}") String secret, UserService userService) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.userService = userService;
    }

    public record DevLoginRequest(String userId) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String userId, String username) {}
    public record RegisterRequest(String username, String password, String previousUserId) {}
    public record LoginRequest(String username, String password) {}

    @PostMapping("/dev-login")
    public TokenResponse devLogin(@RequestBody DevLoginRequest request) {
        TokenResponse token = issueToken(request.userId(), null);
        // Kept for backward compatibility with any client still calling
        // this; username is unknown/absent for a bare device id.
        return token;
    }

    @PostMapping("/register")
    public TokenResponse register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        try {
            User user = userService.register(request.username(), request.password(), request.previousUserId());
            return issueToken(user.getId().toString(), user.getUsername());
        } catch (UserService.UsernameTakenException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.username(), request.password());
            return issueToken(user.getId().toString(), user.getUsername());
        } catch (UserService.InvalidCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    private TokenResponse issueToken(String userId, String username) {
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
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new TokenResponse(token, "Bearer", ChronoUnit.SECONDS.between(now, expiry), userId, username);
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