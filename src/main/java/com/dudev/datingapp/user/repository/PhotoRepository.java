package com.dudev.datingapp.user.repository;

import com.dudev.datingapp.user.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findByUserIdOrderByPosition(UUID userId);

    Optional<Photo> findFirstByUserIdOrderByPositionAsc(UUID userId);

    int countByUserId(UUID userId);

    Optional<Photo> findByIdAndUserId(UUID id, UUID userId);

    void deleteAllByUserId(UUID userId);
}
