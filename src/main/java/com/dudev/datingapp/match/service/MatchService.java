package com.dudev.datingapp.match.service;

import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.common.exception.UnauthorizedException;
import com.dudev.datingapp.match.dto.MatchDetailDto;
import com.dudev.datingapp.match.dto.MatchSummaryDto;
import com.dudev.datingapp.match.dto.UpdateMatchStatusRequest;
import com.dudev.datingapp.match.entity.Match;
import com.dudev.datingapp.match.entity.MatchStatus;
import com.dudev.datingapp.match.event.MatchEvent;
import com.dudev.datingapp.match.repository.MatchRepository;
import com.dudev.datingapp.plan.entity.EveningPlan;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import com.dudev.datingapp.swipe.event.SwipeEvent;
import com.dudev.datingapp.topic.dto.TopicDto;
import com.dudev.datingapp.topic.service.TopicService;
import com.dudev.datingapp.user.repository.PhotoRepository;
import com.dudev.datingapp.user.service.PhotoStorageService;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    public static final String MATCH_EVENTS_TOPIC = "match-events";

    private final MatchRepository matchRepository;
    private final EveningPlanRepository planRepository;
    private final VenueRepository venueRepository;
    private final TopicService topicService;
    private final PhotoRepository photoRepository;
    private final PhotoStorageService photoStorageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /// Creates the match if it doesn't already exist. Returns the matchId
    /// either way so the caller (sync swipe path) can return it to the client.
    @Transactional
    public UUID createMatchIfAbsent(SwipeEvent event) {
        return matchRepository
                .findByUsersAndDate(event.swiperId(), event.swipedId(), event.date())
                .map(Match::getId)
                .orElseGet(() -> {
                    EveningPlan plan1 = planRepository
                            .findByUserIdAndVenueIdAndDate(event.swiperId(), event.venueId(), event.date())
                            .orElseThrow(() -> new ResourceNotFoundException("Plan not found for swiper"));
                    EveningPlan plan2 = planRepository
                            .findByUserIdAndVenueIdAndDate(event.swipedId(), event.venueId(), event.date())
                            .orElseThrow(() -> new ResourceNotFoundException("Plan not found for swiped user"));

                    Match match = new Match();
                    match.setUser1Id(event.swiperId());
                    match.setUser2Id(event.swipedId());
                    match.setVenueId(event.venueId());
                    match.setPlan1Id(plan1.getId());
                    match.setPlan2Id(plan2.getId());
                    match.setDate(event.date());
                    matchRepository.save(match);

                    kafkaTemplate.send(MATCH_EVENTS_TOPIC, match.getId().toString(),
                            new MatchEvent(match.getId(), match.getUser1Id(), match.getUser2Id(),
                                    match.getVenueId(), match.getDate()));
                    return match.getId();
                });
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryDto> getMatches(UUID userId) {
        List<Match> matches = matchRepository.findAllForUser(userId);
        if (matches.isEmpty()) {
            return List.of();
        }

        // Each row needs the *partner's* bar so the user knows where to go.
        // match.venueId is the swiper's bar, which can differ now that
        // discovery spans a 2km radius.
        Set<UUID> partnerPlanIds = matches.stream()
                .map(m -> userId.equals(m.getUser1Id()) ? m.getPlan2Id() : m.getPlan1Id())
                .collect(Collectors.toSet());
        Map<UUID, EveningPlan> planById = planRepository.findAllById(partnerPlanIds).stream()
                .collect(Collectors.toMap(EveningPlan::getId, Function.identity()));

        return matches.stream()
                .map(m -> {
                    UUID partnerPlanId = userId.equals(m.getUser1Id()) ? m.getPlan2Id() : m.getPlan1Id();
                    EveningPlan partnerPlan = planById.get(partnerPlanId);
                    Venue venue = partnerPlan != null ? partnerPlan.getVenue() : null;
                    return new MatchSummaryDto(m.getId(),
                            venue != null ? venue.getId() : m.getVenueId(),
                            venue != null ? venue.getName() : null,
                            m.getDate(), m.getStatus());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchDetailDto getMatchDetail(UUID userId, UUID matchId) {
        Match match = matchRepository.findByIdForUser(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));

        UUID partnerUserId = userId.equals(match.getUser1Id()) ? match.getUser2Id() : match.getUser1Id();
        UUID partnerPlanId = userId.equals(match.getUser1Id()) ? match.getPlan2Id() : match.getPlan1Id();
        EveningPlan partnerPlan = planRepository.findById(partnerPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner plan not found"));

        // Use the partner's plan venue, not match.venueId (which is the
        // requester's bar). With the cross-bar discovery radius the two
        // can differ — and the user needs to know where to *go*.
        Venue venue = venueRepository.findById(partnerPlan.getVenue().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        List<String> tags = topicService.findByIds(partnerPlan.getTopicIds()).stream()
                .flatMap(t -> t.tags().stream())
                .distinct()
                .toList();

        List<String> partnerPhotoUrls = photoRepository.findByUserIdOrderByPosition(partnerUserId).stream()
                .map(p -> photoStorageService.toUrl(p.getS3Key()))
                .toList();

        return new MatchDetailDto(match.getId(), venue.getId(), venue.getName(),
                venue.getAddress(), match.getDate(), match.getStatus(),
                partnerPlan.getDrinkTonight(), tags, partnerPlan.getAppearanceHint(),
                partnerPhotoUrls);
    }

    @Transactional
    public MatchSummaryDto updateStatus(UUID userId, UUID matchId, UpdateMatchStatusRequest req) {
        Match match = matchRepository.findByIdForUser(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));

        if (match.getStatus() == MatchStatus.EXPIRED) {
            throw new UnauthorizedException("Cannot update an expired match");
        }

        match.setStatus(req.status());
        matchRepository.save(match);

        Venue venue = venueRepository.findById(match.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        return new MatchSummaryDto(match.getId(), venue.getId(), venue.getName(),
                match.getDate(), match.getStatus());
    }
}
