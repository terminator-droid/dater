package com.dudev.datingapp.swipe.dto;

import com.dudev.datingapp.swipe.entity.SwipeDirection;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwipeRequest(
        @NotNull UUID swipedId,
        @NotNull UUID venueId,
        @NotNull SwipeDirection direction
) {
}
