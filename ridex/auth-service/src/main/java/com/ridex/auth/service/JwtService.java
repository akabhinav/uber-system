package com.ridex.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final StringRedisTemplate redisTemplate;

    @Value("${ridex.jwt.secret}")
    private String jwtSecret;

    @Value("${ridex.jwt.expiry-minutes:60}")
    private int expiryMinutes;

    public JwtService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateToken(UUID userId, String phone) {
        return JWT.create()
            .withSubject(userId.toString())
            .withClaim("phone", phone)
            .withIssuedAt(Instant.now())
            .withExpiresAt(Instant.now().plus(Duration.ofMinutes(expiryMinutes)))
            .withIssuer("ridex-auth")
            .sign(Algorithm.HMAC256(jwtSecret));
    }

    public DecodedJWT verifyToken(String token) {
        try {
            // Check blacklist
            if (Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + token))) {
                throw new JWTVerificationException("Token is blacklisted");
            }
            return JWT.require(Algorithm.HMAC256(jwtSecret))
                .withIssuer("ridex-auth")
                .build()
                .verify(token);
        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            throw e;
        }
    }

    public void blacklistToken(String token) {
        redisTemplate.opsForValue().set(
            "jwt:blacklist:" + token, "1",
            Duration.ofMinutes(expiryMinutes));
    }
}
