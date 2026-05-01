package com.dudev.datingapp.notification.consumer;

import com.dudev.datingapp.match.event.MatchEvent;
import com.dudev.datingapp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "match-events", containerFactory = "matchEventListenerContainerFactory")
    public void onMatchEvent(MatchEvent event) {
        notificationService.onMatchCreated(event);
    }
}
