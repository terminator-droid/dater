package com.dudev.datingapp.security;

import com.dudev.datingapp.auth.controller.AuthController;
import com.dudev.datingapp.auth.dto.AuthResponse;
import com.dudev.datingapp.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean OnlineStatusInterceptor onlineStatusInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(onlineStatusInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("access", "refresh"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+79001234567","password":"password123",\
                                "name":"Alice","birthDate":"1995-01-01","gender":"FEMALE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    @Test
    void register_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+79001234567","name":"Alice",\
                                "birthDate":"1995-01-01","gender":"FEMALE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+79001234567","password":"short",\
                                "name":"Alice","birthDate":"1995-01-01","gender":"FEMALE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validRequest_returns200() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("access", "refresh"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+79001234567","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }
}
