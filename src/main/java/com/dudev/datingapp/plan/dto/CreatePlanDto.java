package com.dudev.datingapp.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePlanDto(
        @NotNull UUID venueId,
        @NotNull LocalDate date,
        @NotBlank String drinkTonight,
        @NotNull @Size(min = 1, max = 5) List<String> topicIds
) {
}
