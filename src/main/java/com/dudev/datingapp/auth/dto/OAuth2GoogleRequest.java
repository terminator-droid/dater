package com.dudev.datingapp.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuth2GoogleRequest(@NotBlank String idToken) {
}
