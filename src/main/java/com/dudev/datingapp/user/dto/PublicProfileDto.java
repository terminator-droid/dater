package com.dudev.datingapp.user.dto;

import java.util.List;
import java.util.UUID;

public record PublicProfileDto(
        UUID id,
        List<PhotoDto> photos
) {
}
