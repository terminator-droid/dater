package com.dudev.datingapp.auth.dto;

import com.dudev.datingapp.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank String phone,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String name,
        @NotNull LocalDate birthDate,
        @NotNull Gender gender
) {
}
