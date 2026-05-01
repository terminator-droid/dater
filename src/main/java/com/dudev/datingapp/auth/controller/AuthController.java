package com.dudev.datingapp.auth.controller;

import com.dudev.datingapp.auth.dto.AuthResponse;
import com.dudev.datingapp.auth.dto.LoginRequest;
import com.dudev.datingapp.auth.dto.OAuth2GoogleRequest;
import com.dudev.datingapp.auth.dto.RefreshRequest;
import com.dudev.datingapp.auth.dto.RegisterRequest;
import com.dudev.datingapp.auth.service.AuthService;
import com.dudev.datingapp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/oauth2/google")
    public ApiResponse<AuthResponse> loginWithGoogle(@Valid @RequestBody OAuth2GoogleRequest request) {
        return ApiResponse.ok(authService.loginWithGoogle(request.idToken()));
    }
}
