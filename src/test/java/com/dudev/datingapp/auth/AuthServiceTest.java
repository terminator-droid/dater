package com.dudev.datingapp.auth;

import com.dudev.datingapp.auth.dto.AuthResponse;
import com.dudev.datingapp.auth.dto.LoginRequest;
import com.dudev.datingapp.auth.dto.RegisterRequest;
import com.dudev.datingapp.auth.service.AuthService;
import com.dudev.datingapp.common.exception.ConflictException;
import com.dudev.datingapp.common.exception.UnauthorizedException;
import com.dudev.datingapp.security.JwtProperties;
import com.dudev.datingapp.security.JwtService;
import com.dudev.datingapp.user.entity.Gender;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private JwtProperties props;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        props = new JwtProperties(
                "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007",
                Duration.ofMinutes(15),
                Duration.ofDays(30)
        );
        jwtService = new JwtService(props);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, props, redisTemplate);
    }

    @Test
    void register_savesUserAndReturnsTokens() {
        when(userRepository.existsByPhone("+79001234567")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        AuthResponse response = authService.register(registerRequest());

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsConflictWhenPhoneExists() {
        when(userRepository.existsByPhone("+79001234567")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokensForValidCredentials() {
        User user = userWithEncodedPassword();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        AuthResponse response = authService.login(new LoginRequest("+79001234567", "password123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void login_throwsBadCredentialsForWrongPassword() {
        User user = userWithEncodedPassword();
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("+79001234567", "wrongpassword")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsBadCredentialsForUnknownPhone() {
        when(userRepository.findByPhone(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("+79009999999", "password123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_returnsNewTokensForValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("refresh:valid-token")).thenReturn(userId.toString());

        AuthResponse response = authService.refresh("valid-token");

        assertThat(response.accessToken()).isNotBlank();
        verify(redisTemplate).delete("refresh:valid-token");
    }

    @Test
    void refresh_throwsUnauthorizedForExpiredToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest("+79001234567", "password123", "Иван",
                LocalDate.of(1995, 6, 15), Gender.MALE);
    }

    private User userWithEncodedPassword() {
        User user = new User();
        user.setPhone("+79001234567");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setName("Иван");
        return user;
    }
}
