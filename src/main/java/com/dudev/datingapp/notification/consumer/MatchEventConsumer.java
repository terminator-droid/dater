package com.dudev.datingapp.notification.consumer;

import com.dudev.datingapp.match.event.MatchEvent;
import com.dudev.datingapp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "match-events", containerFactory = "matchEventListenerContainerFactory")
    public void onMatchEvent(MatchEvent event) {
        try {
            notificationService.onMatchCreated(event);
        } catch (Exception e) {
            log.error("Failed to process match event {}: {}", event, e.getMessage(), e);
        }
    }
}
