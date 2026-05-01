package com.dudev.datingapp.user.dto;

import com.dudev.datingapp.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateProfileDto(
        @NotBlank String name,
        LocalDate birthDate,
        Gender gender
) {
}
