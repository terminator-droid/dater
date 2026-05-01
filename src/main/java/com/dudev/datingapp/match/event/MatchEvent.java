package com.dudev.datingapp.match.event;

import java.time.LocalDate;
import java.util.UUID;

public record MatchEvent(
        UUID matchId,
        UUID user1Id,
        UUID user2Id,
        UUID venueId,
        LocalDate date
) {
}
