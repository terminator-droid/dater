package com.dudev.datingapp.plan.repository;

import com.dudev.datingapp.plan.entity.EveningPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EveningPlanRepository extends JpaRepository<EveningPlan, UUID> {

    List<EveningPlan> findByUserIdAndDate(UUID userId, LocalDate date);

    boolean existsByUserIdAndVenueIdAndDate(UUID userId, UUID venueId, LocalDate date);

    Optional<EveningPlan> findByIdAndUserId(UUID id, UUID userId);

    List<EveningPlan> findByUserIdInAndVenueIdAndDate(Collection<UUID> userIds, UUID venueId, LocalDate date);

    Optional<EveningPlan> findByUserIdAndVenueIdAndDate(UUID userId, UUID venueId, LocalDate date);
}
