package com.dudev.datingapp.plan.repository;

import com.dudev.datingapp.plan.entity.EveningPlan;
import com.dudev.datingapp.plan.entity.PlanStatus;
import com.dudev.datingapp.user.entity.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EveningPlanRepository extends JpaRepository<EveningPlan, UUID> {

    List<EveningPlan> findByUserIdAndDate(UUID userId, LocalDate date);

    boolean existsByUserIdAndVenueIdAndDate(UUID userId, UUID venueId, LocalDate date);

    boolean existsByUserIdAndDateAndStatus(UUID userId, LocalDate date, PlanStatus status);

    Optional<EveningPlan> findByIdAndUserId(UUID id, UUID userId);

    List<EveningPlan> findByUserIdInAndVenueIdAndDate(Collection<UUID> userIds, UUID venueId, LocalDate date);

    Optional<EveningPlan> findByUserIdAndVenueIdAndDate(UUID userId, UUID venueId, LocalDate date);

    void deleteAllByUserId(UUID userId);

    @Query("""
            select distinct p from EveningPlan p
            left join fetch p.venue
            left join fetch p.topicIds
            where p.user.id = :userId and p.date = :date
            """)
    List<EveningPlan> findByUserIdAndDateWithVenueAndTopics(UUID userId, LocalDate date);

    @Query("""
            select distinct p from EveningPlan p
            left join fetch p.venue
            left join fetch p.topicIds
            where p.user.id = :userId
            order by p.date desc
            """)
    List<EveningPlan> findAllByUserIdWithVenueAndTopics(UUID userId);

    @Modifying
    @Query("""
            update EveningPlan p
            set p.status = com.dudev.datingapp.plan.entity.PlanStatus.PLANNED
            where p.user.id = :userId
              and p.date = :date
              and p.status = com.dudev.datingapp.plan.entity.PlanStatus.ACTIVE
              and p.id <> :exceptId
            """)
    int deactivateOtherActivePlans(@Param("userId") UUID userId,
                                   @Param("date") LocalDate date,
                                   @Param("exceptId") UUID exceptId);

    @Query("""
            select p
            from EveningPlan p
            where p.user.id in :userIds
              and p.venue.id = :venueId
              and p.date = :date
              and p.user.gender <> :gender
            """)
    List<EveningPlan> findByUserIdInAndVenueIdAndDateAndUserGenderNot(
            List<UUID> userIds,
            UUID venueId,
            LocalDate date,
            Gender gender
    );

    /// Wider variant used by Discovery: returns plans for the given users on
    /// the given date regardless of venue, since candidates inside the geo
    /// radius may be at a different bar than the requester. Eagerly fetches
    /// venue + user so the service doesn't N+1 when building DTOs.
    @Query("""
            select distinct p
            from EveningPlan p
            join fetch p.venue
            join fetch p.user
            where p.user.id in :userIds
              and p.date = :date
              and p.user.gender <> :gender
            """)
    List<EveningPlan> findByUserIdInAndDateAndUserGenderNot(
            List<UUID> userIds,
            LocalDate date,
            Gender gender
    );

    /// Past plans with no matches attached — safe to hard-delete because the
    /// cleanup job won't break match history. Plans tied to a match stay
    /// forever (the match references plan1Id/plan2Id as a NOT NULL FK).
    @Query("""
            select p from EveningPlan p
            where p.date < :cutoff
              and size(p.matches1) = 0
              and size(p.matches2) = 0
            """)
    List<EveningPlan> findOrphanPastPlans(LocalDate cutoff);
}
