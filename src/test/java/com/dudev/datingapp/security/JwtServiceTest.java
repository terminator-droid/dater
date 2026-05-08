package com.dudev.datingapp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007";

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(30));
        jwtService = new JwtService(props);
    }

    @Test
    void generateAccessToken_returnsNonBlankToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUserId_returnsOriginalUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void isValid_returnsTrueForFreshToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID());
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        JwtProperties expiredProps = new JwtProperties(SECRET, Duration.ofMillis(1), Duration.ofDays(30));
        JwtService expiredService = new JwtService(expiredProps);
        String token = expiredService.generateAccessToken(UUID.randomUUID());

        // give the token time to expire
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(expiredService.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForGarbage() {
        assertThat(jwtService.isValid("not.a.token")).isFalse();
    }
}
