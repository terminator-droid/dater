package com.dudev.datingapp.plan.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.plan.dto.CreatePlanDto;
import com.dudev.datingapp.plan.dto.PlanDto;
import com.dudev.datingapp.plan.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Plans", description = "Evening plans — going out tonight")
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an evening plan for a venue and date")
    public ApiResponse<PlanDto> createPlan(Authentication auth,
                                            @Valid @RequestBody CreatePlanDto dto) {
        return ApiResponse.ok(planService.createPlan(currentUserId(auth), dto));
    }

    @GetMapping
    @Operation(summary = "Get plans — all plans of the user, or only those on a given date")
    public ApiResponse<List<PlanDto>> getPlans(
            Authentication auth,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID userId = currentUserId(auth);
        List<PlanDto> plans = (date == null)
                ? planService.getAllPlans(userId)
                : planService.getPlans(userId, date);
        return ApiResponse.ok(plans);
    }

    @PostMapping("/{planId}/activate")
    @Operation(summary = "Activate a plan; deactivates any other active plan on the same date")
    public ApiResponse<PlanDto> activatePlan(Authentication auth, @PathVariable UUID planId) {
        return ApiResponse.ok(planService.activatePlan(currentUserId(auth), planId));
    }

    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel an evening plan (cascades to dependent matches)")
    public void deletePlan(Authentication auth, @PathVariable UUID planId) {
        planService.deletePlan(currentUserId(auth), planId);
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
