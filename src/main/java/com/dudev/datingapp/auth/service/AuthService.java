package com.dudev.datingapp.auth.service;

import com.dudev.datingapp.auth.dto.AuthResponse;
import com.dudev.datingapp.auth.dto.LoginRequest;
import com.dudev.datingapp.auth.dto.RegisterRequest;
import com.dudev.datingapp.common.exception.ConflictException;
import com.dudev.datingapp.common.exception.UnauthorizedException;
import com.dudev.datingapp.security.JwtProperties;
import com.dudev.datingapp.security.JwtService;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String GOOGLE_TOKENINFO_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.phone())) {
            throw new ConflictException("Phone already registered");
        }
        User user = new User();
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setBirthDate(request.birthDate());
        user.setGender(request.gender());
        userRepository.save(user);
        return issueTokens(user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return issueTokens(user.getId());
    }

    public AuthResponse refresh(String refreshToken) {
        String key = REFRESH_PREFIX + refreshToken;
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (userIdStr == null) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        redisTemplate.delete(key);
        return issueTokens(UUID.fromString(userIdStr));
    }

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenInfo info = verifyGoogleToken(idToken);
        User user = userRepository.findByGoogleSub(info.sub())
                .orElseGet(() -> createOrLinkGoogleUser(info));
        return issueTokens(user.getId());
    }

    private GoogleTokenInfo verifyGoogleToken(String idToken) {
        try {
            GoogleTokenInfo info = RestClient.builder().build().get()
                    .uri(GOOGLE_TOKENINFO_URL + idToken)
                    .retrieve()
                    .body(GoogleTokenInfo.class);
            if (info == null || info.sub() == null) {
                throw new UnauthorizedException("Invalid Google token");
            }
            return info;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid Google token");
        }
    }

    private User createOrLinkGoogleUser(GoogleTokenInfo info) {
        if (info.email() != null) {
            var existing = userRepository.findByPhone(info.email());
            if (existing.isPresent()) {
                User u = existing.get();
                u.setGoogleSub(info.sub());
                return userRepository.save(u);
            }
        }
        User user = new User();
        user.setGoogleSub(info.sub());
        user.setPhone("google:" + info.sub());
        user.setPassword("");
        user.setName(info.name() != null ? info.name() : "Google User");
        return userRepository.save(user);
    }

    private record GoogleTokenInfo(String sub, String email, String name) {
    }

    private AuthResponse issueTokens(UUID userId) {
        String accessToken = jwtService.generateAccessToken(userId);
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + refreshToken,
                userId.toString(),
                jwtProperties.refreshTokenTtl()
        );
        return new AuthResponse(accessToken, refreshToken);
    }
}
