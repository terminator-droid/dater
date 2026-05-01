package com.dudev.datingapp.plan.dto;

import com.dudev.datingapp.plan.entity.PlanStatus;
import com.dudev.datingapp.venue.dto.VenueDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlanDto(
        UUID id,
        VenueDto venue,
        LocalDate date,
        String drinkTonight,
        PlanStatus status,
        List<String> topicIds
) {
}
