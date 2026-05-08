package com.dudev.datingapp.user.dto;

import com.dudev.datingapp.user.entity.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String phone,
        String name,
        LocalDate birthDate,
        Gender gender,
        List<PhotoDto> photos
) {
}
