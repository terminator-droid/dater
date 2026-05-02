package com.dudev.datingapp.swipe.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.swipe.dto.SwipeRequest;
import com.dudev.datingapp.swipe.dto.SwipeResponse;
import com.dudev.datingapp.swipe.service.SwipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/swipes")
@RequiredArgsConstructor
@Tag(name = "Swipes", description = "Like or pass on a discovery card")
public class SwipeController {

    private final SwipeService swipeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Swipe LIKE or PASS on a user at a venue")
    public ApiResponse<SwipeResponse> swipe(Authentication auth,
                                             @Valid @RequestBody SwipeRequest req) {
        return ApiResponse.ok(swipeService.swipe(currentUserId(auth), req));
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
