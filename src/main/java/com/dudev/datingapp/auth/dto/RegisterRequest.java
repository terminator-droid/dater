package com.dudev.datingapp.auth.dto;

import com.dudev.datingapp.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Phone must be in international format, e.g. +79001234567")
        String phone,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String name,
        @NotNull @Past(message = "Birth date must be in the past") LocalDate birthDate,
        @NotNull Gender gender
) {
}
