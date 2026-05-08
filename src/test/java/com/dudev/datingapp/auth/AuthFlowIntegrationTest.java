package com.dudev.datingapp.auth;

import com.dudev.datingapp.TestcontainersConfiguration;
import com.dudev.datingapp.auth.dto.LoginRequest;
import com.dudev.datingapp.auth.dto.RegisterRequest;
import com.dudev.datingapp.auth.service.AuthService;
import com.dudev.datingapp.common.exception.ConflictException;
import com.dudev.datingapp.user.entity.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class AuthFlowIntegrationTest {

    @Autowired
    AuthService authService;

    @Test
    void register_login_refresh_allSucceed() {
        var reg = authService.register(new RegisterRequest(
                "+79001111001", "pass123", "Alice", LocalDate.of(1995, 1, 1), Gender.FEMALE));
        assertNotNull(reg.accessToken());
        assertNotNull(reg.refreshToken());

        var login = authService.login(new LoginRequest("+79001111001", "pass123"));
        assertNotNull(login.accessToken());

        var refresh = authService.refresh(reg.refreshToken());
        assertNotNull(refresh.accessToken());
        assertNotEquals(reg.refreshToken(), refresh.refreshToken());
    }

    @Test
    void register_duplicatePhone_throwsConflict() {
        authService.register(new RegisterRequest(
                "+79001111002", "pass", "Bob", LocalDate.of(1990, 5, 10), Gender.MALE));
        assertThrows(ConflictException.class, () ->
                authService.register(new RegisterRequest(
                        "+79001111002", "other", "BobDup", LocalDate.of(1990, 5, 10), Gender.MALE)));
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        authService.register(new RegisterRequest(
                "+79001111003", "correct", "Carol", LocalDate.of(1993, 3, 3), Gender.FEMALE));
        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("+79001111003", "wrong")));
    }
}
