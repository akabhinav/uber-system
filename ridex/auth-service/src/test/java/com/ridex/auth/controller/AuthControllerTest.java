package com.ridex.auth.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ridex.auth.model.User;
import com.ridex.auth.repository.UserRepository;
import com.ridex.auth.service.JwtService;
import com.ridex.auth.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private OtpService otpService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(otpService, jwtService, userRepository);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    // --- POST /api/auth/otp/send ---

    @Test
    void sendOtp_shouldReturnOtp() {
        when(otpService.generateOtp("+1234567890")).thenReturn("123456");

        webTestClient.post()
                .uri("/api/auth/otp/send")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"+1234567890\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.message").isEqualTo("OTP sent")
                .jsonPath("$.data.otp_dev").isEqualTo("123456");

        verify(otpService).generateOtp("+1234567890");
    }

    @Test
    void sendOtp_shouldCallOtpServiceWithCorrectPhone() {
        when(otpService.generateOtp("+9876543210")).thenReturn("654321");

        webTestClient.post()
                .uri("/api/auth/otp/send")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"+9876543210\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.otp_dev").isEqualTo("654321");

        verify(otpService).generateOtp("+9876543210");
    }

    // --- POST /api/auth/otp/verify ---

    @Test
    void verifyOtp_shouldReturnToken_whenOtpIsValid_andUserExists() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "+1234567890", "Test User");

        when(otpService.verifyOtp("+1234567890", "123456")).thenReturn(true);
        when(userRepository.findByPhone("+1234567890")).thenReturn(Mono.just(user));
        when(jwtService.generateToken(userId, "+1234567890")).thenReturn("jwt-token-123");

        webTestClient.post()
                .uri("/api/auth/otp/verify")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"+1234567890\",\"otp\":\"123456\",\"name\":\"Test User\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.token").isEqualTo("jwt-token-123")
                .jsonPath("$.data.userId").isEqualTo(userId.toString())
                .jsonPath("$.data.phone").isEqualTo("+1234567890")
                .jsonPath("$.data.name").isEqualTo("Test User");
    }

    @Test
    void verifyOtp_shouldCreateNewUser_whenUserDoesNotExist() {
        when(otpService.verifyOtp("+1234567890", "123456")).thenReturn(true);
        when(userRepository.findByPhone("+1234567890")).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return Mono.just(savedUser);
        });
        when(jwtService.generateToken(any(UUID.class), eq("+1234567890"))).thenReturn("new-user-token");

        webTestClient.post()
                .uri("/api/auth/otp/verify")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"+1234567890\",\"otp\":\"123456\",\"name\":\"New User\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.token").isEqualTo("new-user-token")
                .jsonPath("$.data.phone").isEqualTo("+1234567890")
                .jsonPath("$.data.name").isEqualTo("New User");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyOtp_shouldReturnError_whenOtpIsInvalid() {
        when(otpService.verifyOtp("+1234567890", "000000")).thenReturn(false);

        webTestClient.post()
                .uri("/api/auth/otp/verify")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"+1234567890\",\"otp\":\"000000\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.errorCode").isEqualTo("INVALID_OTP")
                .jsonPath("$.error").isEqualTo("Invalid or expired OTP");

        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void verifyOtp_shouldSetEmptyName_whenNameIsNull() {
        when(otpService.verifyOtp("+1234567890", "123456")).thenReturn(true);
        when(userRepository.findByPhone("+1234567890")).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            return Mono.just(savedUser);
        });
        when(jwtService.generateToken(any(UUID.class), eq("+1234567890"))).thenReturn("token");

        webTestClient.post()
                .uri("/api/auth/otp/verify")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"+1234567890\",\"otp\":\"123456\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.name").isEqualTo("");
    }

    @Test
    void verifyOtp_shouldReturnEmptyName_whenExistingUserHasNullName() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "+1234567890", null);

        when(otpService.verifyOtp("+1234567890", "123456")).thenReturn(true);
        when(userRepository.findByPhone("+1234567890")).thenReturn(Mono.just(user));
        when(jwtService.generateToken(userId, "+1234567890")).thenReturn("token");

        webTestClient.post()
                .uri("/api/auth/otp/verify")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\":\"+1234567890\",\"otp\":\"123456\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("");
    }

    // --- POST /api/auth/logout ---

    @Test
    void logout_shouldBlacklistTokenAndReturnSuccess() {
        webTestClient.post()
                .uri("/api/auth/logout")
                .header("Authorization", "Bearer jwt-token-to-blacklist")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.message").isEqualTo("Logged out successfully");

        verify(jwtService).blacklistToken("jwt-token-to-blacklist");
    }

    @Test
    void logout_shouldStripBearerPrefixFromToken() {
        webTestClient.post()
                .uri("/api/auth/logout")
                .header("Authorization", "Bearer my-jwt-token")
                .exchange()
                .expectStatus().isOk();

        verify(jwtService).blacklistToken("my-jwt-token");
    }

    // --- GET /api/auth/me ---

    @Test
    void getCurrentUser_shouldReturnUserData_whenTokenIsValid() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "+1234567890", "John Doe");
        user.setActive(true);

        DecodedJWT decodedJWT = createDecodedJwt(userId, "+1234567890");
        when(jwtService.verifyToken("valid-token")).thenReturn(decodedJWT);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));

        webTestClient.get()
                .uri("/api/auth/me")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.userId").isEqualTo(userId.toString())
                .jsonPath("$.data.name").isEqualTo("John Doe")
                .jsonPath("$.data.phone").isEqualTo("+1234567890")
                .jsonPath("$.data.active").isEqualTo(true);
    }

    @Test
    void getCurrentUser_shouldReturnError_whenUserNotFound() {
        UUID userId = UUID.randomUUID();
        DecodedJWT decodedJWT = createDecodedJwt(userId, "+1234567890");
        when(jwtService.verifyToken("valid-token")).thenReturn(decodedJWT);
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/auth/me")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.errorCode").isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void getCurrentUser_shouldReturnEmptyName_whenUserNameIsNull() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "+1234567890", null);
        user.setActive(true);

        DecodedJWT decodedJWT = createDecodedJwt(userId, "+1234567890");
        when(jwtService.verifyToken("valid-token")).thenReturn(decodedJWT);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));

        webTestClient.get()
                .uri("/api/auth/me")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("");
    }

    @Test
    void getCurrentUser_shouldReturn500_whenTokenIsInvalid() {
        when(jwtService.verifyToken("invalid-token"))
                .thenThrow(new JWTVerificationException("Token verification failed"));

        webTestClient.get()
                .uri("/api/auth/me")
                .header("Authorization", "Bearer invalid-token")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    // --- Helper methods ---

    private User createTestUser(UUID id, String phone, String name) {
        User user = new User();
        user.setId(id);
        user.setPhone(phone);
        user.setName(name);
        user.setCreatedAt(Instant.now());
        user.setActive(true);
        return user;
    }

    private DecodedJWT createDecodedJwt(UUID userId, String phone) {
        String token = JWT.create()
                .withSubject(userId.toString())
                .withClaim("phone", phone)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(Duration.ofMinutes(60)))
                .withIssuer("ridex-auth")
                .sign(Algorithm.HMAC256("test-secret"));
        return JWT.decode(token);
    }
}
