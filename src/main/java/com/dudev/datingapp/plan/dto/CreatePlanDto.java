package com.dudev.datingapp.plan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePlanDto(
        @NotNull UUID venueId,
        @NotNull LocalDate date,
        // Optional — users may not have picked a drink yet. Empty/null is fine.
        String drinkTonight,
        // Optional conversation topics; capped at 5 to keep the discovery
        // card legible.
        @Size(max = 5) List<String> topicIds,
        @Size(max = 200) String appearanceHint
) {
}
