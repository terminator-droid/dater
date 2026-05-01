package com.dudev.datingapp.match;

import com.dudev.datingapp.TestcontainersConfiguration;
import com.dudev.datingapp.auth.dto.RegisterRequest;
import com.dudev.datingapp.auth.service.AuthService;
import com.dudev.datingapp.match.entity.Match;
import com.dudev.datingapp.match.entity.MatchStatus;
import com.dudev.datingapp.match.repository.MatchRepository;
import com.dudev.datingapp.plan.dto.CreatePlanDto;
import com.dudev.datingapp.plan.service.PlanService;
import com.dudev.datingapp.security.JwtService;
import com.dudev.datingapp.swipe.consumer.SwipeEventConsumer;
import com.dudev.datingapp.swipe.dto.SwipeRequest;
import com.dudev.datingapp.swipe.entity.SwipeDirection;
import com.dudev.datingapp.swipe.event.SwipeEvent;
import com.dudev.datingapp.swipe.service.SwipeService;
import com.dudev.datingapp.user.entity.Gender;
import com.dudev.datingapp.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class MatchCreationIntegrationTest {

    @MockitoBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired AuthService authService;
    @Autowired PlanService planService;
    @Autowired SwipeService swipeService;
    @Autowired SwipeEventConsumer swipeEventConsumer;
    @Autowired MatchRepository matchRepository;
    @Autowired VenueRepository venueRepository;
    @Autowired JwtService jwtService;

    @Test
    void mutualLike_createsMatchWithCorrectStatus() {
        UUID id1 = registerUser("+79002221001", "Dave", Gender.MALE);
        UUID id2 = registerUser("+79002221002", "Eve", Gender.FEMALE);

        UUID venueId = venueRepository.findAll().get(0).getId();
        LocalDate today = LocalDate.now();

        planService.createPlan(id1, new CreatePlanDto(venueId, today, "Beer", List.of()));
        planService.createPlan(id2, new CreatePlanDto(venueId, today, "Wine", List.of()));

        swipeService.swipe(id1, new SwipeRequest(id2, venueId, SwipeDirection.LIKE));
        swipeService.swipe(id2, new SwipeRequest(id1, venueId, SwipeDirection.LIKE));

        // Trigger consumer directly — bypasses Kafka async timing
        swipeEventConsumer.onSwipeEvent(new SwipeEvent(id2, id1, venueId, today, SwipeDirection.LIKE));

        List<Match> matches = matchRepository.findAllForUser(id1);
        assertFalse(matches.isEmpty(), "Match should be created");
        assertEquals(MatchStatus.PENDING, matches.get(0).getStatus());
    }

    @Test
    void passSwipe_doesNotCreateMatch() {
        UUID id1 = registerUser("+79002221003", "Frank", Gender.MALE);
        UUID id2 = registerUser("+79002221004", "Grace", Gender.FEMALE);

        UUID venueId = venueRepository.findAll().get(0).getId();
        LocalDate today = LocalDate.now();

        planService.createPlan(id1, new CreatePlanDto(venueId, today, "Beer", List.of()));
        planService.createPlan(id2, new CreatePlanDto(venueId, today, "Wine", List.of()));

        swipeService.swipe(id1, new SwipeRequest(id2, venueId, SwipeDirection.PASS));

        // Consumer processes the PASS event — should do nothing
        swipeEventConsumer.onSwipeEvent(new SwipeEvent(id1, id2, venueId, today, SwipeDirection.PASS));

        assertTrue(matchRepository.findAllForUser(id1).isEmpty());
    }

    private UUID registerUser(String phone, String name, Gender gender) {
        var auth = authService.register(new RegisterRequest(
                phone, "pass123", name, LocalDate.of(1995, 1, 1), gender));
        return jwtService.extractUserId(auth.accessToken());
    }
}
