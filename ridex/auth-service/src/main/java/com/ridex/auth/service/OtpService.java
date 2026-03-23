package com.ridex.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    @Value("${ridex.otp.ttl-seconds:300}")
    private int otpTtlSeconds;

    @Value("${ridex.otp.length:6}")
    private int otpLength;

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateOtp(String phone) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        String otpStr = otp.toString();
        String key = "otp:" + phone;

        Boolean wasSet = redisTemplate.opsForValue()
            .setIfAbsent(key, otpStr, Duration.ofSeconds(otpTtlSeconds));

        if (Boolean.FALSE.equals(wasSet)) {
            // OTP already exists, return existing one
            String existing = redisTemplate.opsForValue().get(key);
            log.info("OTP already exists for phone={}", phone);
            return existing;
        }

        log.info("Generated OTP for phone={}, otp={}", phone, otpStr);
        return otpStr;
    }

    public boolean verifyOtp(String phone, String otp) {
        String key = "otp:" + phone;
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            redisTemplate.delete(key);
            log.info("OTP verified for phone={}", phone);
            return true;
        }
        log.warn("OTP verification failed for phone={}", phone);
        return false;
    }
}
