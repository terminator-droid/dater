package com.dudev.datingapp.venue.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.venue.dto.VenueDto;
import com.dudev.datingapp.venue.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Venues", description = "Bars and venues catalogue")
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    @Operation(summary = "List all venues, optionally filtered by area")
    public ApiResponse<List<VenueDto>> findAll(@RequestParam(required = false) String area) {
        return ApiResponse.ok(venueService.findAll(area));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get venue by ID")
    public ApiResponse<VenueDto> findById(@PathVariable UUID id) {
        return ApiResponse.ok(venueService.findById(id));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find venues within radiusKm of a coordinate (default 2 km)")
    public ApiResponse<List<VenueDto>> findNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "2.0") double radiusKm) {
        return ApiResponse.ok(venueService.findNearby(lat, lon, radiusKm));
    }
}
