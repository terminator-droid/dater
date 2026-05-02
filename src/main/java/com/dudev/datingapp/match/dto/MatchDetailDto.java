package com.dudev.datingapp.match.dto;

import com.dudev.datingapp.match.entity.MatchStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MatchDetailDto(
        UUID matchId,
        UUID venueId,
        String venueName,
        String venueAddress,
        LocalDate date,
        MatchStatus status,
        String partnerDrinkTonight,
        List<String> partnerTopicTags,
        String partnerAppearanceHint
) {
}
