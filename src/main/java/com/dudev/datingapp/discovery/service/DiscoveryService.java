package com.dudev.datingapp.discovery.service;

import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.discovery.dto.DiscoveryCardDto;
import com.dudev.datingapp.plan.entity.EveningPlan;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import com.dudev.datingapp.plan.service.PlanService;
import com.dudev.datingapp.swipe.service.SwipeService;
import com.dudev.datingapp.topic.dto.TopicDto;
import com.dudev.datingapp.topic.service.TopicService;
import com.dudev.datingapp.user.entity.Gender;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.PhotoRepository;
import com.dudev.datingapp.user.repository.UserRepository;
import com.dudev.datingapp.user.service.PhotoStorageService;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final VenueRepository venueRepository;
    private final EveningPlanRepository planRepository;
    private final PhotoRepository photoRepository;
    private final TopicService topicService;
    private final PhotoStorageService photoStorageService;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    /// Search radius — Discovery shows the requester's bar plus any other
    /// venue within this distance, so users staying nearby still appear.
    private static final double DISCOVERY_RADIUS_KM = 2.0;

    @Transactional(readOnly = true)
    public List<DiscoveryCardDto> discover(UUID currentUserId, UUID venueId, LocalDate date) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        String geoKey = PlanService.GEO_KEY_PREFIX + date;
        Circle circle = new Circle(
                new Point(venue.getLongitude(), venue.getLatitude()),
                new Distance(DISCOVERY_RADIUS_KM, org.springframework.data.geo.Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                redisTemplate.opsForGeo().radius(geoKey, circle);

        if (geoResults == null) {
            return List.of();
        }

        Set<String> alreadySwiped = Optional
                .ofNullable(redisTemplate.opsForSet()
                        .members(SwipeService.SWIPED_SET_PREFIX + currentUserId + ":" + date))
                .orElse(Set.of());

        List<UUID> candidateIds = geoResults.getContent().stream()
                .map(r -> r.getContent().getName())
                .filter(id -> !id.equals(currentUserId.toString()))
                .filter(id -> !alreadySwiped.contains(id))
                .map(UUID::fromString)
                .toList();

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Candidates can be at *any* venue inside the radius — pull each
        // user's own active plan for the date so we know where they actually
        // are, not where the requester is.
        Map<UUID, EveningPlan> planByUserId = planRepository
                .findByUserIdInAndDateAndUserGenderNot(candidateIds, date, currentUser.getGender())
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(),
                        // If a user has multiple plans on the date, prefer the active one.
                        (a, b) -> a.getStatus() == com.dudev.datingapp.plan.entity.PlanStatus.ACTIVE ? a : b));

        List<String> allTopicIds = planByUserId.values().stream()
                .flatMap(p -> p.getTopicIds().stream())
                .distinct()
                .toList();

        Map<String, List<String>> tagsByTopicId = topicService.findByIds(allTopicIds).stream()
                .collect(Collectors.toMap(TopicDto::id, TopicDto::tags));

        return candidateIds.stream()
                .filter(planByUserId::containsKey)
                .map(userId -> {
                    EveningPlan plan = planByUserId.get(userId);
                    Venue partnerVenue = plan.getVenue();
                    List<String> tags = plan.getTopicIds().stream()
                            .flatMap(tid -> tagsByTopicId.getOrDefault(tid, List.of()).stream())
                            .distinct()
                            .toList();
                    List<String> photoUrls = photoRepository.findByUserIdOrderByPosition(userId).stream()
                            .map(p -> photoStorageService.toUrl(p.getS3Key()))
                            .toList();
                    String photoUrl = photoUrls.isEmpty() ? null : photoUrls.get(0);
                    return new DiscoveryCardDto(
                            userId, photoUrl, photoUrls, tags,
                            partnerVenue.getId(), partnerVenue.getName(), partnerVenue.getAddress());
                })
                .toList();
    }
}
