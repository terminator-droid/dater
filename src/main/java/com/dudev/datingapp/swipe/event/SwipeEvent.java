package com.dudev.datingapp.swipe.event;

import com.dudev.datingapp.swipe.entity.SwipeDirection;

import java.time.LocalDate;
import java.util.UUID;

public record SwipeEvent(
        UUID swiperId,
        UUID swipedId,
        UUID venueId,
        LocalDate date,
        SwipeDirection direction
) {
}
