package com.dudev.datingapp.plan.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.plan.dto.CreatePlanDto;
import com.dudev.datingapp.plan.dto.PlanDto;
import com.dudev.datingapp.plan.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanDto> createPlan(Authentication auth,
                                            @Valid @RequestBody CreatePlanDto dto) {
        return ApiResponse.ok(planService.createPlan(currentUserId(auth), dto));
    }

    @GetMapping
    public ApiResponse<List<PlanDto>> getPlans(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(planService.getPlans(currentUserId(auth), date));
    }

    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(Authentication auth, @PathVariable UUID planId) {
        planService.deletePlan(currentUserId(auth), planId);
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
