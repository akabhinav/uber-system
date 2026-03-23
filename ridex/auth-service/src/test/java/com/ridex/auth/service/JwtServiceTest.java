package com.ridex.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hmac256";
    private static final int EXPIRY_MINUTES = 60;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService(redisTemplate);
        setField(jwtService, "jwtSecret", TEST_SECRET);
        setField(jwtService, "expiryMinutes", EXPIRY_MINUTES);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // --- generateToken tests ---

    @Test
    void generateToken_shouldReturnValidJwt() {
        UUID userId = UUID.randomUUID();
        String phone = "+1234567890";

        String token = jwtService.generateToken(userId, phone);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        // JWT has 3 dot-separated parts
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_shouldIncludeUserIdAsSubject() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "+1234567890");

        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
    }

    @Test
    void generateToken_shouldIncludePhoneClaim() {
        UUID userId = UUID.randomUUID();
        String phone = "+1234567890";
        String token = jwtService.generateToken(userId, phone);

        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getClaim("phone").asString()).isEqualTo(phone);
    }

    @Test
    void generateToken_shouldSetIssuerToRidexAuth() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "+1234567890");

        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getIssuer()).isEqualTo("ridex-auth");
    }

    @Test
    void generateToken_shouldSetExpiryInTheFuture() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "+1234567890");

        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getExpiresAtAsInstant()).isAfter(Instant.now());
    }

    @Test
    void generateToken_shouldSetIssuedAtToNow() {
        UUID userId = UUID.randomUUID();
        Instant before = Instant.now().minusSeconds(1);
        String token = jwtService.generateToken(userId, "+1234567890");
        Instant after = Instant.now().plusSeconds(1);

        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getIssuedAtAsInstant()).isBetween(before, after);
    }

    // --- verifyToken tests ---

    @Test
    void verifyToken_shouldReturnDecodedJwt_forValidToken() {
        UUID userId = UUID.randomUUID();
        String phone = "+1234567890";
        String token = jwtService.generateToken(userId, phone);

        when(redisTemplate.hasKey("jwt:blacklist:" + token)).thenReturn(Boolean.FALSE);

        DecodedJWT decoded = jwtService.verifyToken(token);

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaim("phone").asString()).isEqualTo(phone);
    }

    @Test
    void verifyToken_shouldThrow_forBlacklistedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "+1234567890");

        when(redisTemplate.hasKey("jwt:blacklist:" + token)).thenReturn(Boolean.TRUE);

        assertThatThrownBy(() -> jwtService.verifyToken(token))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("blacklisted");
    }

    @Test
    void verifyToken_shouldThrow_forTokenWithWrongSecret() {
        // Create a token signed with a different secret
        String token = JWT.create()
                .withSubject(UUID.randomUUID().toString())
                .withClaim("phone", "+1234567890")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(Duration.ofMinutes(60)))
                .withIssuer("ridex-auth")
                .sign(Algorithm.HMAC256("wrong-secret"));

        when(redisTemplate.hasKey("jwt:blacklist:" + token)).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> jwtService.verifyToken(token))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void verifyToken_shouldThrow_forExpiredToken() throws Exception {
        // Create an already-expired token
        String token = JWT.create()
                .withSubject(UUID.randomUUID().toString())
                .withClaim("phone", "+1234567890")
                .withIssuedAt(Instant.now().minus(Duration.ofHours(2)))
                .withExpiresAt(Instant.now().minus(Duration.ofHours(1)))
                .withIssuer("ridex-auth")
                .sign(Algorithm.HMAC256(TEST_SECRET));

        when(redisTemplate.hasKey("jwt:blacklist:" + token)).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> jwtService.verifyToken(token))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void verifyToken_shouldThrow_forTokenWithWrongIssuer() {
        String token = JWT.create()
                .withSubject(UUID.randomUUID().toString())
                .withClaim("phone", "+1234567890")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(Duration.ofMinutes(60)))
                .withIssuer("wrong-issuer")
                .sign(Algorithm.HMAC256(TEST_SECRET));

        when(redisTemplate.hasKey("jwt:blacklist:" + token)).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> jwtService.verifyToken(token))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void verifyToken_shouldThrow_forMalformedToken() {
        String malformedToken = "not.a.valid.jwt";

        when(redisTemplate.hasKey("jwt:blacklist:" + malformedToken)).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> jwtService.verifyToken(malformedToken))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void verifyToken_shouldCheckBlacklistBeforeVerifying() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "+1234567890");

        when(redisTemplate.hasKey("jwt:blacklist:" + token)).thenReturn(Boolean.TRUE);

        assertThatThrownBy(() -> jwtService.verifyToken(token))
                .isInstanceOf(JWTVerificationException.class)
                .hasMessageContaining("blacklisted");

        verify(redisTemplate).hasKey("jwt:blacklist:" + token);
    }

    // --- blacklistToken tests ---

    @Test
    void blacklistToken_shouldStoreTokenInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = "some.jwt.token";
        jwtService.blacklistToken(token);

        verify(valueOperations).set(
                eq("jwt:blacklist:" + token),
                eq("1"),
                eq(Duration.ofMinutes(EXPIRY_MINUTES))
        );
    }

    @Test
    void blacklistToken_shouldUseTtlMatchingTokenExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        jwtService.blacklistToken("test-token");

        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofMinutes(60)));
    }
}
