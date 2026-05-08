package com.dudev.datingapp.match.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.match.dto.MatchDetailDto;
import com.dudev.datingapp.match.dto.MatchSummaryDto;
import com.dudev.datingapp.match.dto.UpdateMatchStatusRequest;
import com.dudev.datingapp.match.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Mutual likes — match list and detail")
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    @Operation(summary = "Get own match list")
    public ApiResponse<List<MatchSummaryDto>> getMatches(Authentication auth) {
        return ApiResponse.ok(matchService.getMatches(currentUserId(auth)));
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Get match detail: partner drink + topics (no name/age)")
    public ApiResponse<MatchDetailDto> getMatchDetail(Authentication auth, @PathVariable UUID matchId) {
        return ApiResponse.ok(matchService.getMatchDetail(currentUserId(auth), matchId));
    }

    @PatchMapping("/{matchId}/status")
    @Operation(summary = "Update match status (e.g. mark as MET)")
    public ApiResponse<MatchSummaryDto> updateStatus(Authentication auth,
                                                      @PathVariable UUID matchId,
                                                      @Valid @RequestBody UpdateMatchStatusRequest req) {
        return ApiResponse.ok(matchService.updateStatus(currentUserId(auth), matchId, req));
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
