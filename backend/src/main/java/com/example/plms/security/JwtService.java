package com.example.plms.security;

import com.example.plms.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final int ttlHours;

    public JwtService(AuthProperties properties) {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("PLMS_JWT_SECRET is not configured");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("PLMS_JWT_SECRET must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.ttlHours = properties.getTokenTtlHours();
    }

    public String generateToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttlHours, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();
    }

    public Optional<Long> parseUserId(String token) {
        try {
            Jws<Claims> claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            String subject = claims.getPayload().getSubject();
            return subject == null ? Optional.empty() : Optional.of(Long.valueOf(subject));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
