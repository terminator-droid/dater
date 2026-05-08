package com.dudev.datingapp.user.service;

import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.match.repository.MatchRepository;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import com.dudev.datingapp.plan.service.PlanService;
import com.dudev.datingapp.swipe.repository.SwipeRepository;
import com.dudev.datingapp.user.dto.LocationUpdateDto;
import com.dudev.datingapp.user.dto.PhotoDto;
import com.dudev.datingapp.user.dto.PublicProfileDto;
import com.dudev.datingapp.user.dto.UpdateProfileDto;
import com.dudev.datingapp.user.dto.UserProfileDto;
import com.dudev.datingapp.user.entity.Photo;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.PhotoRepository;
import com.dudev.datingapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    private final PhotoStorageService storageService;
    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final EveningPlanRepository planRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        User user = findUser(userId);
        List<Photo> photos = photoRepository.findByUserIdOrderByPosition(userId);
        return toDto(user, photos);
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileDto dto) {
        if (dto.birthDate().isAfter(LocalDate.now().minusYears(18))) {
            throw new IllegalArgumentException("You must be at least 18 years old");
        }
        User user = findUser(userId);
        user.setName(dto.name());
        user.setBirthDate(dto.birthDate());
        user.setGender(dto.gender());
        userRepository.save(user);
        List<Photo> photos = photoRepository.findByUserIdOrderByPosition(userId);
        return toDto(user, photos);
    }

    @Transactional
    public PhotoDto addPhoto(UUID userId, MultipartFile file) throws IOException {
        User user = findUser(userId);
        String key = storageService.store(userId, file);

        int position = photoRepository.countByUserId(userId);
        boolean isPrimary = position == 0;

        Photo photo = new Photo();
        photo.setUser(user);
        photo.setS3Key(key);
        photo.setPosition(position);
        photo.setPrimary(isPrimary);
        photoRepository.save(photo);

        return toPhotoDto(photo);
    }

    @Transactional
    public void deletePhoto(UUID userId, UUID photoId) throws IOException {
        Photo photo = photoRepository.findByIdAndUserId(photoId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found"));
        storageService.delete(photo.getS3Key());
        photoRepository.delete(photo);
        reorderPhotos(userId);
    }

    @Transactional(readOnly = true)
    public PublicProfileDto getPublicProfile(UUID userId) {
        User user = findUser(userId);
        List<Photo> photos = photoRepository.findByUserIdOrderByPosition(userId);
        return new PublicProfileDto(user.getId(), photos.stream().map(this::toPhotoDto).toList());
    }

    public boolean isOnline(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("user:" + userId + ":online"));
    }

    public void updateLocation(UUID userId, LocationUpdateDto dto) {
        String key = PlanService.GEO_KEY_PREFIX + LocalDate.now();
        redisTemplate.opsForGeo().add(key,
                new Point(dto.longitude(), dto.latitude()),
                userId.toString());
        Instant midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Duration ttl = Duration.between(Instant.now(), midnight);
        if (!ttl.isNegative()) {
            redisTemplate.expire(key, ttl);
        }
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        List<Photo> photos = photoRepository.findByUserIdOrderByPosition(userId);
        for (Photo p : photos) {
            try {
                storageService.delete(p.getS3Key());
            } catch (IOException ignored) {
            }
        }
        photoRepository.deleteAllByUserId(userId);
        swipeRepository.deleteAllBySwiperIdOrSwipedId(userId, userId);
        matchRepository.deleteAllByUserId(userId);
        planRepository.deleteAllByUserId(userId);
        redisTemplate.delete("user:" + userId + ":online");
        userRepository.deleteById(userId);
    }

    private void reorderPhotos(UUID userId) {
        List<Photo> photos = photoRepository.findByUserIdOrderByPosition(userId);
        for (int i = 0; i < photos.size(); i++) {
            Photo p = photos.get(i);
            p.setPosition(i);
            p.setPrimary(i == 0);
        }
        photoRepository.saveAll(photos);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileDto toDto(User user, List<Photo> photos) {
        return new UserProfileDto(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getBirthDate(),
                user.getGender(),
                photos.stream().map(this::toPhotoDto).toList()
        );
    }

    private PhotoDto toPhotoDto(Photo photo) {
        return new PhotoDto(
                photo.getId(),
                storageService.toUrl(photo.getS3Key()),
                photo.getPosition(),
                photo.isPrimary()
        );
    }
}
