package com.dudev.datingapp.match;

import com.dudev.datingapp.match.dto.MatchDetailDto;
import com.dudev.datingapp.match.dto.MatchSummaryDto;
import com.dudev.datingapp.match.dto.UpdateMatchStatusRequest;
import com.dudev.datingapp.match.entity.Match;
import com.dudev.datingapp.match.entity.MatchStatus;
import com.dudev.datingapp.match.event.MatchEvent;
import com.dudev.datingapp.match.repository.MatchRepository;
import com.dudev.datingapp.match.service.MatchService;
import com.dudev.datingapp.plan.entity.EveningPlan;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import com.dudev.datingapp.swipe.entity.SwipeDirection;
import com.dudev.datingapp.swipe.event.SwipeEvent;
import com.dudev.datingapp.topic.dto.TopicDto;
import com.dudev.datingapp.topic.service.TopicService;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.PhotoRepository;
import com.dudev.datingapp.user.service.PhotoStorageService;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock EveningPlanRepository planRepository;
    @Mock VenueRepository venueRepository;
    @Mock TopicService topicService;
    @Mock PhotoRepository photoRepository;
    @Mock PhotoStorageService photoStorageService;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks MatchService matchService;

    private final UUID swiperId = UUID.randomUUID();
    private final UUID swipedId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    @Test
    void createMatchIfAbsent_newMatch_savesAndPublishesMatchEvent() {
        SwipeEvent event = new SwipeEvent(swiperId, swipedId, venueId, LocalDate.now(), SwipeDirection.LIKE);
        when(matchRepository.findByUsersAndDate(swiperId, swipedId, event.date())).thenReturn(Optional.empty());

        when(planRepository.findByUserIdAndVenueIdAndDate(swiperId, venueId, event.date()))
                .thenReturn(Optional.of(makePlan(swiperId)));
        when(planRepository.findByUserIdAndVenueIdAndDate(swipedId, venueId, event.date()))
                .thenReturn(Optional.of(makePlan(swipedId)));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
            return m;
        });

        matchService.createMatchIfAbsent(event);

        verify(matchRepository).save(any(Match.class));
        verify(kafkaTemplate).send(eq(MatchService.MATCH_EVENTS_TOPIC), anyString(), any(MatchEvent.class));
    }

    @Test
    void createMatchIfAbsent_alreadyExists_skips() {
        SwipeEvent event = new SwipeEvent(swiperId, swipedId, venueId, LocalDate.now(), SwipeDirection.LIKE);
        Match existing = new Match();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        when(matchRepository.findByUsersAndDate(swiperId, swipedId, event.date()))
                .thenReturn(Optional.of(existing));

        matchService.createMatchIfAbsent(event);

        verify(matchRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void getMatchDetail_returnsPartnerDrinkAndTopics() {
        UUID matchId = UUID.randomUUID();
        UUID plan2Id = UUID.randomUUID();

        Match match = new Match();
        ReflectionTestUtils.setField(match, "id", matchId);
        match.setUser1Id(swiperId);
        match.setUser2Id(swipedId);
        match.setVenueId(venueId);
        match.setPlan1Id(UUID.randomUUID());
        match.setPlan2Id(plan2Id);
        match.setDate(LocalDate.now());
        when(matchRepository.findByIdForUser(matchId, swiperId)).thenReturn(Optional.of(match));

        Venue venue = new Venue();
        ReflectionTestUtils.setField(venue, "id", venueId);
        venue.setName("Bar Strelka");

        EveningPlan partnerPlan = makePlan(swipedId);
        partnerPlan.setDrinkTonight("Negroni");
        partnerPlan.setTopicIds(List.of("t1"));
        // Service now uses partnerPlan.venue (cross-bar discovery), not match.venueId.
        partnerPlan.setVenue(venue);
        when(planRepository.findById(plan2Id)).thenReturn(Optional.of(partnerPlan));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        when(topicService.findByIds(List.of("t1"))).thenReturn(
                List.of(new TopicDto("t1", "drink", "text", List.of("вино", "коктейли"))));

        MatchDetailDto detail = matchService.getMatchDetail(swiperId, matchId);

        assertEquals("Negroni", detail.partnerDrinkTonight());
        assertEquals(List.of("вино", "коктейли"), detail.partnerTopicTags());
        assertEquals("Bar Strelka", detail.venueName());
    }

    @Test
    void updateStatus_met_updatesMatchStatus() {
        UUID matchId = UUID.randomUUID();
        Match match = new Match();
        ReflectionTestUtils.setField(match, "id", matchId);
        match.setUser1Id(swiperId);
        match.setUser2Id(swipedId);
        match.setVenueId(venueId);
        match.setDate(LocalDate.now());
        match.setStatus(MatchStatus.PENDING);
        when(matchRepository.findByIdForUser(matchId, swiperId)).thenReturn(Optional.of(match));
        when(matchRepository.save(any())).thenReturn(match);

        Venue venue = new Venue();
        ReflectionTestUtils.setField(venue, "id", venueId);
        venue.setName("Bar Strelka");
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        MatchSummaryDto result = matchService.updateStatus(swiperId, matchId, new UpdateMatchStatusRequest(MatchStatus.MET));

        assertEquals(MatchStatus.MET, result.status());
    }

    private EveningPlan makePlan(UUID userId) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        EveningPlan plan = new EveningPlan();
        ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
        plan.setUser(user);
        return plan;
    }
}
