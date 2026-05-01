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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void createMatchIfAbsent(SwipeEvent event) {
        if (matchRepository.existsByUsersAndDate(event.swiperId(), event.swipedId(), event.date())) {
            return;
        }

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
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryDto> getMatches(UUID userId) {
        List<Match> matches = matchRepository.findAllForUser(userId);
        if (matches.isEmpty()) {
            return List.of();
        }
        Set<UUID> venueIds = matches.stream().map(Match::getVenueId).collect(Collectors.toSet());
        Map<UUID, Venue> venueById = venueRepository.findByIdIn(venueIds).stream()
                .collect(Collectors.toMap(Venue::getId, Function.identity()));

        return matches.stream()
                .map(m -> {
                    Venue venue = venueById.get(m.getVenueId());
                    return new MatchSummaryDto(m.getId(), m.getVenueId(),
                            venue != null ? venue.getName() : null, m.getDate(), m.getStatus());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchDetailDto getMatchDetail(UUID userId, UUID matchId) {
        Match match = matchRepository.findByIdForUser(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));

        UUID partnerPlanId = userId.equals(match.getUser1Id()) ? match.getPlan2Id() : match.getPlan1Id();
        EveningPlan partnerPlan = planRepository.findById(partnerPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner plan not found"));

        Venue venue = venueRepository.findById(match.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        List<String> tags = topicService.findByIds(partnerPlan.getTopicIds()).stream()
                .flatMap(t -> t.tags().stream())
                .distinct()
                .toList();

        return new MatchDetailDto(match.getId(), venue.getId(), venue.getName(),
                match.getDate(), match.getStatus(), partnerPlan.getDrinkTonight(), tags);
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
