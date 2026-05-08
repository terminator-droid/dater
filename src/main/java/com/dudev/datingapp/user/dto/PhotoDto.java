package com.dudev.datingapp.user.dto;

import java.util.UUID;

public record PhotoDto(
        UUID id,
        String url,
        int position,
        boolean primary
) {
}
