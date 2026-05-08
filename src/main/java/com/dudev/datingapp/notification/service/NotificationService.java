package com.dudev.datingapp.notification.service;

import com.dudev.datingapp.match.event.MatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void onMatchCreated(MatchEvent event) {
        log.info("MATCH: matchId={}, users={} <-> {}, venue={}, date={}",
                event.matchId(), event.user1Id(), event.user2Id(), event.venueId(), event.date());
        // TODO: send FCM push notification
    }
}
