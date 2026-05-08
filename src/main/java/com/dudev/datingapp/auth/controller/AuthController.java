package com.dudev.datingapp.auth.controller;

import com.dudev.datingapp.auth.dto.AuthResponse;
import com.dudev.datingapp.auth.dto.LoginRequest;
import com.dudev.datingapp.auth.dto.OAuth2GoogleRequest;
import com.dudev.datingapp.auth.dto.RefreshRequest;
import com.dudev.datingapp.auth.dto.RegisterRequest;
import com.dudev.datingapp.auth.service.AuthService;
import com.dudev.datingapp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, token refresh and OAuth2")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with phone + password")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/oauth2/google")
    @Operation(summary = "Login or register via Google ID token")
    public ApiResponse<AuthResponse> loginWithGoogle(@Valid @RequestBody OAuth2GoogleRequest request) {
        return ApiResponse.ok(authService.loginWithGoogle(request.idToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Logout and invalidate refresh token")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }
}
