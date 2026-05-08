package com.dudev.datingapp.discovery.dto;

import java.util.List;
import java.util.UUID;

/// Each card carries the venue this user is *actually* at tonight. With the
/// wider 2km search radius, the candidate may be in a different bar than the
/// requester — the client uses these fields to surface "they're at Bar Y"
/// before the swipe and to know where to go after a match.
public record DiscoveryCardDto(
        UUID userId,
        // Kept for older clients; equals photoUrls.get(0) when any photo exists.
        String photoUrl,
        List<String> photoUrls,
        List<String> topicTags,
        UUID venueId,
        String venueName,
        String venueAddress
) {
}
