package com.dudev.datingapp.discovery.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.discovery.dto.DiscoveryCardDto;
import com.dudev.datingapp.discovery.service.DiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discover")
@RequiredArgsConstructor
@Tag(name = "Discovery", description = "Anonymous discovery cards at a venue")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @GetMapping
    @Operation(summary = "Get anonymous discovery cards for a venue on a date")
    public ApiResponse<List<DiscoveryCardDto>> discover(
            Authentication auth,
            @RequestParam UUID venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(discoveryService.discover(currentUserId(auth), venueId, date));
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
