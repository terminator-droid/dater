package com.dudev.datingapp.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
