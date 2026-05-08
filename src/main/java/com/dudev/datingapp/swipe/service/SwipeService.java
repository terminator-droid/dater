package com.dudev.datingapp.swipe.service;

import com.dudev.datingapp.common.exception.TooManyRequestsException;
import com.dudev.datingapp.match.service.MatchService;
import com.dudev.datingapp.swipe.dto.SwipeRequest;
import com.dudev.datingapp.swipe.dto.SwipeResponse;
import com.dudev.datingapp.swipe.entity.Swipe;
import com.dudev.datingapp.swipe.entity.SwipeDirection;
import com.dudev.datingapp.swipe.event.SwipeEvent;
import com.dudev.datingapp.swipe.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwipeService {

    public static final int DAILY_SWIPE_LIMIT = 100;
    static final String RATE_KEY_PREFIX = "swipe:rate:";
    public static final String SWIPED_SET_PREFIX = "swiped:";
    public static final String SWIPE_EVENTS_TOPIC = "swipe-events";

    private final SwipeRepository swipeRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MatchService matchService;

    @Transactional
    public SwipeResponse swipe(UUID swiperId, SwipeRequest req) {
        if (swiperId.equals(req.swipedId())) {
            throw new IllegalArgumentException("Cannot swipe yourself");
        }

        LocalDate today = LocalDate.now();
        checkRateLimit(swiperId, today);

        Swipe swipe = new Swipe();
        swipe.setSwiperId(swiperId);
        swipe.setSwipedId(req.swipedId());
        swipe.setVenueId(req.venueId());
        swipe.setDirection(req.direction());
        swipe.setDate(today);
        swipeRepository.save(swipe);

        String swipedKey = SWIPED_SET_PREFIX + swiperId + ":" + today;
        redisTemplate.opsForSet().add(swipedKey, req.swipedId().toString());
        setTtlUntilMidnight(swipedKey, today);

        UUID matchId = null;
        if (req.direction() == SwipeDirection.LIKE) {
            // Synchronous mutual-like check: if the other user already liked
            // us today, create the match inline so the swipe response carries
            // matchId back. The Kafka path is still kept as a safety net for
            // any future async listeners (analytics etc).
            boolean reverseExists = swipeRepository
                    .existsBySwiperIdAndSwipedIdAndDirectionAndDate(
                            req.swipedId(), swiperId, SwipeDirection.LIKE, today);
            if (reverseExists) {
                try {
                    matchId = matchService.createMatchIfAbsent(new SwipeEvent(
                            swiperId, req.swipedId(), req.venueId(), today, SwipeDirection.LIKE));
                } catch (Exception e) {
                    // Don't fail the swipe if match creation throws — the Kafka
                    // consumer will retry. The client just won't get an instant
                    // animation in that edge case.
                    log.warn("Synchronous match creation failed; falling back to async: {}",
                            e.getMessage());
                }
            }
            kafkaTemplate.send(SWIPE_EVENTS_TOPIC, swiperId.toString(),
                    new SwipeEvent(swiperId, req.swipedId(), req.venueId(), today, SwipeDirection.LIKE));
        }

        return new SwipeResponse(swipe.getId(), matchId);
    }

    private void checkRateLimit(UUID userId, LocalDate date) {
        String key = RATE_KEY_PREFIX + userId + ":" + date;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            setTtlUntilMidnight(key, date);
        }
        if (count != null && count > DAILY_SWIPE_LIMIT) {
            throw new TooManyRequestsException("Daily swipe limit reached");
        }
    }

    private void setTtlUntilMidnight(String key, LocalDate date) {
        Instant midnight = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Duration ttl = Duration.between(Instant.now(), midnight);
        if (!ttl.isNegative()) {
            redisTemplate.expire(key, ttl);
        }
    }
}
