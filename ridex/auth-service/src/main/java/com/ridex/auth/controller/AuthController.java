package com.ridex.auth.controller;

import com.ridex.auth.model.User;
import com.ridex.auth.repository.UserRepository;
import com.ridex.auth.service.JwtService;
import com.ridex.auth.service.OtpService;
import com.ridex.commons.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final OtpService otpService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(OtpService otpService, JwtService jwtService, UserRepository userRepository) {
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/otp/send")
    public Mono<ApiResponse<Map<String, String>>> sendOtp(@RequestBody OtpRequest request) {
        String otp = otpService.generateOtp(request.phone());
        log.info("OTP sent to {}: {} (dev mode)", request.phone(), otp);
        return Mono.just(ApiResponse.ok(Map.of("message", "OTP sent", "otp_dev", otp)));
    }

    @PostMapping("/otp/verify")
    public Mono<ApiResponse<Map<String, Object>>> verifyOtp(@RequestBody OtpVerifyRequest request) {
        if (!otpService.verifyOtp(request.phone(), request.otp())) {
            return Mono.just(ApiResponse.error("INVALID_OTP", "Invalid or expired OTP"));
        }

        return userRepository.findByPhone(request.phone())
            .switchIfEmpty(createUser(request.phone(), request.name()))
            .map(user -> {
                String token = jwtService.generateToken(user.getId(), user.getPhone());
                return ApiResponse.ok(Map.of(
                    "token", token,
                    "userId", user.getId().toString(),
                    "name", user.getName() != null ? user.getName() : "",
                    "phone", user.getPhone()
                ));
            });
    }

    @PostMapping("/logout")
    public Mono<ApiResponse<Map<String, String>>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        jwtService.blacklistToken(token);
        return Mono.just(ApiResponse.ok(Map.of("message", "Logged out successfully")));
    }

    @GetMapping("/me")
    public Mono<ApiResponse<Map<String, Object>>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        var decoded = jwtService.verifyToken(token);
        UUID userId = UUID.fromString(decoded.getSubject());
        return userRepository.findById(userId)
            .map(user -> ApiResponse.ok(Map.of(
                "userId", user.getId().toString(),
                "name", user.getName() != null ? user.getName() : "",
                "phone", user.getPhone(),
                "active", user.isActive()
            )))
            .defaultIfEmpty(ApiResponse.error("USER_NOT_FOUND", "User not found"));
    }

    private Mono<User> createUser(String phone, String name) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPhone(phone);
        user.setName(name != null ? name : "");
        user.setCreatedAt(Instant.now());
        user.setActive(true);
        return userRepository.save(user);
    }

    public record OtpRequest(String phone) {}
    public record OtpVerifyRequest(String phone, String otp, String name) {}
}
