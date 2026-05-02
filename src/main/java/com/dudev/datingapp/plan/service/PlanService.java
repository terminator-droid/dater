package com.dudev.datingapp.plan.service;

import com.dudev.datingapp.common.exception.ConflictException;
import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.plan.dto.CreatePlanDto;
import com.dudev.datingapp.plan.dto.PlanDto;
import com.dudev.datingapp.plan.entity.EveningPlan;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.UserRepository;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.repository.VenueRepository;
import com.dudev.datingapp.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    public static final String GEO_KEY_PREFIX = "geo:users:";

    private final EveningPlanRepository planRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final VenueService venueService;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public PlanDto createPlan(UUID userId, CreatePlanDto dto) {
        if (planRepository.existsByUserIdAndVenueIdAndDate(userId, dto.venueId(), dto.date())) {
            throw new ConflictException("Plan for this venue and date already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Venue venue = venueRepository.findById(dto.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        EveningPlan plan = new EveningPlan();
        plan.setUser(user);
        plan.setVenue(venue);
        plan.setDate(dto.date());
        plan.setDrinkTonight(dto.drinkTonight());
        plan.setTopicIds(dto.topicIds());
        plan.setAppearanceHint(dto.appearanceHint());
        planRepository.save(plan);

        registerInGeo(userId, venue, dto.date());

        return toDto(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanDto> getPlans(UUID userId, LocalDate date) {
        return planRepository.findByUserIdAndDate(userId, date)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public void deletePlan(UUID userId, UUID planId) {
        EveningPlan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        removeFromGeo(userId, plan.getDate());
        planRepository.delete(plan);
    }

    private void registerInGeo(UUID userId, Venue venue, LocalDate date) {
        String key = GEO_KEY_PREFIX + date;
        redisTemplate.opsForGeo().add(key,
                new Point(venue.getLongitude(), venue.getLatitude()),
                userId.toString());
        setTtlUntilMidnight(key, date);
    }

    private void removeFromGeo(UUID userId, LocalDate date) {
        String key = GEO_KEY_PREFIX + date;
        redisTemplate.opsForZSet().remove(key, userId.toString());
    }

    private void setTtlUntilMidnight(String key, LocalDate date) {
        Instant midnight = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Duration ttl = Duration.between(Instant.now(), midnight);
        if (!ttl.isNegative()) {
            redisTemplate.expire(key, ttl);
        }
    }

    private PlanDto toDto(EveningPlan plan) {
        return new PlanDto(
                plan.getId(),
                venueService.toDto(plan.getVenue()),
                plan.getDate(),
                plan.getDrinkTonight(),
                plan.getStatus(),
                plan.getTopicIds(),
                plan.getAppearanceHint()
        );
    }
}
