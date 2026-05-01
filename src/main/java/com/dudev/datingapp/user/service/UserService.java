package com.dudev.datingapp.user.service;

import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.user.dto.PhotoDto;
import com.dudev.datingapp.user.dto.UpdateProfileDto;
import com.dudev.datingapp.user.dto.UserProfileDto;
import com.dudev.datingapp.user.entity.Photo;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.PhotoRepository;
import com.dudev.datingapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    private final PhotoStorageService storageService;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        User user = findUser(userId);
        List<Photo> photos = photoRepository.findByUserIdOrderByPosition(userId);
        return toDto(user, photos);
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileDto dto) {
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
