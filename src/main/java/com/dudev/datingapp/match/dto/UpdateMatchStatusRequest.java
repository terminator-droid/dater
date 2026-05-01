package com.dudev.datingapp.match.dto;

import com.dudev.datingapp.match.entity.MatchStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMatchStatusRequest(@NotNull MatchStatus status) {
}
