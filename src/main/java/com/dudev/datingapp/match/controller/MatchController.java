package com.dudev.datingapp.match.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.match.dto.MatchDetailDto;
import com.dudev.datingapp.match.dto.MatchSummaryDto;
import com.dudev.datingapp.match.dto.UpdateMatchStatusRequest;
import com.dudev.datingapp.match.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public ApiResponse<List<MatchSummaryDto>> getMatches(Authentication auth) {
        return ApiResponse.ok(matchService.getMatches(currentUserId(auth)));
    }

    @GetMapping("/{matchId}")
    public ApiResponse<MatchDetailDto> getMatchDetail(Authentication auth, @PathVariable UUID matchId) {
        return ApiResponse.ok(matchService.getMatchDetail(currentUserId(auth), matchId));
    }

    @PatchMapping("/{matchId}/status")
    public ApiResponse<MatchSummaryDto> updateStatus(Authentication auth,
                                                      @PathVariable UUID matchId,
                                                      @Valid @RequestBody UpdateMatchStatusRequest req) {
        return ApiResponse.ok(matchService.updateStatus(currentUserId(auth), matchId, req));
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
