package com.dudev.datingapp.user.dto;

import com.dudev.datingapp.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UpdateProfileDto(
        @NotBlank String name,
        @NotNull @Past(message = "Birth date must be in the past") LocalDate birthDate,
        @NotNull Gender gender
) {
}
