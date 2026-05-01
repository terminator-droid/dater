package com.dudev.datingapp.swipe;

import com.dudev.datingapp.match.service.MatchService;
import com.dudev.datingapp.swipe.consumer.SwipeEventConsumer;
import com.dudev.datingapp.swipe.entity.SwipeDirection;
import com.dudev.datingapp.swipe.event.SwipeEvent;
import com.dudev.datingapp.swipe.repository.SwipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwipeEventConsumerTest {

    @Mock SwipeRepository swipeRepository;
    @Mock MatchService matchService;
    @InjectMocks SwipeEventConsumer consumer;

    private final UUID swiperId = UUID.randomUUID();
    private final UUID swipedId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    @Test
    void onSwipeEvent_like_reverseExists_createsMatch() {
        SwipeEvent event = new SwipeEvent(swiperId, swipedId, venueId, LocalDate.now(), SwipeDirection.LIKE);
        when(swipeRepository.existsBySwiperIdAndSwipedIdAndDirectionAndDate(
                swipedId, swiperId, SwipeDirection.LIKE, event.date())).thenReturn(true);

        consumer.onSwipeEvent(event);

        verify(matchService).createMatchIfAbsent(event);
    }

    @Test
    void onSwipeEvent_like_noReverseSwipe_doesNotCreateMatch() {
        SwipeEvent event = new SwipeEvent(swiperId, swipedId, venueId, LocalDate.now(), SwipeDirection.LIKE);
        when(swipeRepository.existsBySwiperIdAndSwipedIdAndDirectionAndDate(
                any(), any(), any(), any())).thenReturn(false);

        consumer.onSwipeEvent(event);

        verifyNoInteractions(matchService);
    }

    @Test
    void onSwipeEvent_pass_doesNothing() {
        SwipeEvent event = new SwipeEvent(swiperId, swipedId, venueId, LocalDate.now(), SwipeDirection.PASS);

        consumer.onSwipeEvent(event);

        verifyNoInteractions(swipeRepository, matchService);
    }
}
