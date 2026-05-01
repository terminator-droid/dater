package com.dudev.datingapp.swipe.service;

import com.dudev.datingapp.common.exception.TooManyRequestsException;
import com.dudev.datingapp.swipe.dto.SwipeRequest;
import com.dudev.datingapp.swipe.dto.SwipeResponse;
import com.dudev.datingapp.swipe.entity.Swipe;
import com.dudev.datingapp.swipe.entity.SwipeDirection;
import com.dudev.datingapp.swipe.event.SwipeEvent;
import com.dudev.datingapp.swipe.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

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

        if (req.direction() == SwipeDirection.LIKE) {
            kafkaTemplate.send(SWIPE_EVENTS_TOPIC, swiperId.toString(),
                    new SwipeEvent(swiperId, req.swipedId(), req.venueId(), today, SwipeDirection.LIKE));
        }

        return new SwipeResponse(swipe.getId());
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
