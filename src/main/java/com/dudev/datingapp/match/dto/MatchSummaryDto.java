package com.dudev.datingapp.match.dto;

import com.dudev.datingapp.match.entity.MatchStatus;

import java.time.LocalDate;
import java.util.UUID;

public record MatchSummaryDto(
        UUID matchId,
        UUID venueId,
        String venueName,
        LocalDate date,
        MatchStatus status
) {
}
