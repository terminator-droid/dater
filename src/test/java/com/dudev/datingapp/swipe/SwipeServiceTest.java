package com.dudev.datingapp.swipe;

import com.dudev.datingapp.common.exception.TooManyRequestsException;
import com.dudev.datingapp.swipe.dto.SwipeRequest;
import com.dudev.datingapp.swipe.dto.SwipeResponse;
import com.dudev.datingapp.swipe.entity.Swipe;
import com.dudev.datingapp.swipe.entity.SwipeDirection;
import com.dudev.datingapp.swipe.event.SwipeEvent;
import com.dudev.datingapp.swipe.repository.SwipeRepository;
import com.dudev.datingapp.swipe.service.SwipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwipeServiceTest {

    @Mock
    SwipeRepository swipeRepository;
    @Mock
    RedisTemplate<String, String> redisTemplate;
    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    SetOperations<String, String> setOps;
    @Mock
    ValueOperations<String, String> valueOps;

    @InjectMocks
    SwipeService swipeService;

    private final UUID swiperId = UUID.randomUUID();
    private final UUID swipedId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    @Test
    void swipe_like_savesAndPublishesToKafka() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(swipeRepository.save(any(Swipe.class))).thenAnswer(inv -> {
            Swipe s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
            return s;
        });

        SwipeResponse resp = swipeService.swipe(swiperId, new SwipeRequest(swipedId, venueId, SwipeDirection.LIKE));

        assertNotNull(resp.swipeId());
        verify(kafkaTemplate).send(
                eq(SwipeService.SWIPE_EVENTS_TOPIC),
                eq(swiperId.toString()),
                any(SwipeEvent.class));
        verify(setOps).add(anyString(), eq(swipedId.toString()));
    }

    @Test
    void swipe_pass_doesNotPublishToKafka() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(swipeRepository.save(any(Swipe.class))).thenAnswer(inv -> {
            Swipe s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
            return s;
        });

        swipeService.swipe(swiperId, new SwipeRequest(swipedId, venueId, SwipeDirection.PASS));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void swipe_self_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> swipeService.swipe(swiperId, new SwipeRequest(swiperId, venueId, SwipeDirection.LIKE)));
        verifyNoInteractions(swipeRepository, kafkaTemplate);
    }

    @Test
    void swipe_rateLimitExceeded_throwsTooManyRequests() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn((long) SwipeService.DAILY_SWIPE_LIMIT + 1);

        assertThrows(TooManyRequestsException.class,
                () -> swipeService.swipe(swiperId, new SwipeRequest(swipedId, venueId, SwipeDirection.LIKE)));
        verifyNoInteractions(swipeRepository, kafkaTemplate);
    }
}
