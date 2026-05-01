package com.dudev.datingapp.discovery.dto;

import java.util.List;
import java.util.UUID;

public record DiscoveryCardDto(
        UUID userId,
        String photoUrl,
        List<String> topicTags
) {
}
