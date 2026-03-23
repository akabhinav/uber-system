package com.ridex.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OtpService otpService;

    @BeforeEach
    void setUp() throws Exception {
        otpService = new OtpService(redisTemplate);

        // Set @Value fields via reflection
        setField(otpService, "otpTtlSeconds", 300);
        setField(otpService, "otpLength", 6);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // --- generateOtp tests ---

    @Test
    void generateOtp_shouldReturnSixDigitOtp_whenNoExistingOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("otp:+1234567890"), anyString(), eq(Duration.ofSeconds(300))))
                .thenReturn(Boolean.TRUE);

        String otp = otpService.generateOtp("+1234567890");

        assertThat(otp).isNotNull();
        assertThat(otp).hasSize(6);
        assertThat(otp).matches("\\d{6}");

        verify(valueOperations).setIfAbsent(eq("otp:+1234567890"), anyString(), eq(Duration.ofSeconds(300)));
    }

    @Test
    void generateOtp_shouldReturnExistingOtp_whenOtpAlreadyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("otp:+1234567890"), anyString(), eq(Duration.ofSeconds(300))))
                .thenReturn(Boolean.FALSE);
        when(valueOperations.get("otp:+1234567890")).thenReturn("123456");

        String otp = otpService.generateOtp("+1234567890");

        assertThat(otp).isEqualTo("123456");
        verify(valueOperations).get("otp:+1234567890");
    }

    @Test
    void generateOtp_shouldUseCorrectRedisKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        otpService.generateOtp("+9876543210");

        verify(valueOperations).setIfAbsent(eq("otp:+9876543210"), anyString(), any(Duration.class));
    }

    @Test
    void generateOtp_shouldUseTtlFromConfig() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        otpService.generateOtp("+1234567890");

        verify(valueOperations).setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(300)));
    }

    @Test
    void generateOtp_shouldGenerateOnlyDigits() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        // Generate multiple OTPs to increase confidence
        for (int i = 0; i < 20; i++) {
            String otp = otpService.generateOtp("+100000000" + i);
            assertThat(otp).matches("\\d{6}");
        }
    }

    // --- verifyOtp tests ---

    @Test
    void verifyOtp_shouldReturnTrue_whenOtpMatches() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:+1234567890")).thenReturn("654321");
        when(redisTemplate.delete("otp:+1234567890")).thenReturn(Boolean.TRUE);

        boolean result = otpService.verifyOtp("+1234567890", "654321");

        assertThat(result).isTrue();
        verify(redisTemplate).delete("otp:+1234567890");
    }

    @Test
    void verifyOtp_shouldReturnFalse_whenOtpDoesNotMatch() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:+1234567890")).thenReturn("654321");

        boolean result = otpService.verifyOtp("+1234567890", "000000");

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyOtp_shouldReturnFalse_whenNoOtpStored() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:+1234567890")).thenReturn(null);

        boolean result = otpService.verifyOtp("+1234567890", "654321");

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyOtp_shouldDeleteOtpFromRedis_afterSuccessfulVerification() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:+1234567890")).thenReturn("123456");
        when(redisTemplate.delete("otp:+1234567890")).thenReturn(Boolean.TRUE);

        otpService.verifyOtp("+1234567890", "123456");

        verify(redisTemplate).delete("otp:+1234567890");
    }

    @Test
    void verifyOtp_shouldUseCorrectRedisKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:+5551234")).thenReturn(null);

        otpService.verifyOtp("+5551234", "111111");

        verify(valueOperations).get("otp:+5551234");
    }
}
