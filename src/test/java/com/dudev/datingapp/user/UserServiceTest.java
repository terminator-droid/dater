package com.dudev.datingapp.user;

import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.match.repository.MatchRepository;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import com.dudev.datingapp.swipe.repository.SwipeRepository;
import com.dudev.datingapp.user.dto.UpdateProfileDto;
import com.dudev.datingapp.user.dto.UserProfileDto;
import com.dudev.datingapp.user.entity.Gender;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.PhotoRepository;
import com.dudev.datingapp.user.repository.UserRepository;
import com.dudev.datingapp.user.service.PhotoStorageService;
import com.dudev.datingapp.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PhotoRepository photoRepository;
    @Mock private PhotoStorageService storageService;
    @Mock private SwipeRepository swipeRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private EveningPlanRepository planRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, photoRepository, storageService,
                swipeRepository, matchRepository, planRepository, redisTemplate);
    }

    @Test
    void getProfile_returnsUserProfile() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(photoRepository.findByUserIdOrderByPosition(userId)).thenReturn(Collections.emptyList());

        UserProfileDto profile = userService.getProfile(userId);

        assertThat(profile.id()).isEqualTo(userId);
        assertThat(profile.phone()).isEqualTo("+79001234567");
        assertThat(profile.name()).isEqualTo("Иван");
        assertThat(profile.gender()).isEqualTo(Gender.MALE);
    }

    @Test
    void getProfile_throwsNotFoundForUnknownUser() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_updatesFieldsAndReturnsDto() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(photoRepository.findByUserIdOrderByPosition(userId)).thenReturn(Collections.emptyList());

        UpdateProfileDto dto = new UpdateProfileDto("Дмитрий", LocalDate.of(1993, 3, 10), Gender.MALE);
        UserProfileDto result = userService.updateProfile(userId, dto);

        assertThat(result.name()).isEqualTo("Дмитрий");
        assertThat(result.birthDate()).isEqualTo(LocalDate.of(1993, 3, 10));
        verify(userRepository).save(user);
    }

    private User buildUser(UUID userId) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setPhone("+79001234567");
        user.setPassword("hashed");
        user.setName("Иван");
        user.setBirthDate(LocalDate.of(1995, 6, 15));
        user.setGender(Gender.MALE);
        return user;
    }
}
