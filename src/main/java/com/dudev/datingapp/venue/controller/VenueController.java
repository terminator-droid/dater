package com.dudev.datingapp.venue.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.venue.dto.VenueDto;
import com.dudev.datingapp.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public ApiResponse<List<VenueDto>> findAll(@RequestParam(required = false) String area) {
        return ApiResponse.ok(venueService.findAll(area));
    }

    @GetMapping("/{id}")
    public ApiResponse<VenueDto> findById(@PathVariable UUID id) {
        return ApiResponse.ok(venueService.findById(id));
    }
}
