package com.dudev.datingapp.swipe.consumer;

import com.dudev.datingapp.match.service.MatchService;
import com.dudev.datingapp.swipe.entity.SwipeDirection;
import com.dudev.datingapp.swipe.event.SwipeEvent;
import com.dudev.datingapp.swipe.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwipeEventConsumer {

    private final SwipeRepository swipeRepository;
    private final MatchService matchService;

    @KafkaListener(topics = "swipe-events", containerFactory = "swipeEventListenerContainerFactory")
    public void onSwipeEvent(SwipeEvent event) {
        try {
            if (event.direction() != SwipeDirection.LIKE) {
                return;
            }
            boolean reverseExists = swipeRepository.existsBySwiperIdAndSwipedIdAndDirectionAndDate(
                    event.swipedId(), event.swiperId(), SwipeDirection.LIKE, event.date());
            if (reverseExists) {
                log.info("Mutual like detected: {} <-> {}", event.swiperId(), event.swipedId());
                matchService.createMatchIfAbsent(event);
            }
        } catch (Exception e) {
            log.error("Failed to process swipe event {}: {}", event, e.getMessage(), e);
        }
    }
}
