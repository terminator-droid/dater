package com.dudev.datingapp.discovery;

import com.dudev.datingapp.discovery.dto.DiscoveryCardDto;
import com.dudev.datingapp.discovery.service.DiscoveryService;
import com.dudev.datingapp.plan.entity.EveningPlan;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import com.dudev.datingapp.topic.dto.TopicDto;
import com.dudev.datingapp.topic.service.TopicService;
import com.dudev.datingapp.user.entity.Gender;
import com.dudev.datingapp.user.entity.Photo;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.PhotoRepository;
import com.dudev.datingapp.user.repository.UserRepository;
import com.dudev.datingapp.user.service.PhotoStorageService;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
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
class DiscoveryServiceTest {

    @Mock VenueRepository venueRepository;
    @Mock EveningPlanRepository planRepository;
    @Mock PhotoRepository photoRepository;
    @Mock TopicService topicService;
    @Mock PhotoStorageService photoStorageService;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock GeoOperations<String, String> geoOps;
    @Mock SetOperations<String, String> setOps;
    @Mock UserRepository userRepository;

    @InjectMocks
    DiscoveryService discoveryService;

    @Test
    void discover_returnsCardsFilteringSelfAndSwiped() {
        UUID venueId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID swipedUserId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        Venue venue = new Venue();
        venue.setLatitude(55.76);
        venue.setLongitude(37.64);
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        when(redisTemplate.opsForGeo()).thenReturn(geoOps);
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(List.of(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>(currentUserId.toString(), null), new Distance(0)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>(candidateId.toString(), null), new Distance(50)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>(swipedUserId.toString(), null), new Distance(80))
        ));
        when(geoOps.radius(anyString(), any(Circle.class))).thenReturn(geoResults);

        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(anyString())).thenReturn(Set.of(swipedUserId.toString()));

        User currentUser = new User();
        currentUser.setGender(Gender.MALE);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        User candidateUser = new User();
        ReflectionTestUtils.setField(candidateUser, "id", candidateId);
        candidateUser.setGender(Gender.FEMALE);
        Venue candidateVenue = new Venue();
        ReflectionTestUtils.setField(candidateVenue, "id", UUID.randomUUID());
        candidateVenue.setName("Bar Y");
        candidateVenue.setAddress("ул. Тверская, 5");
        EveningPlan plan = new EveningPlan();
        plan.setUser(candidateUser);
        plan.setVenue(candidateVenue);
        plan.setTopicIds(List.of("t1", "t2"));
        when(planRepository.findByUserIdInAndDateAndUserGenderNot(
                List.of(candidateId), date, Gender.MALE))
                .thenReturn(List.of(plan));

        when(topicService.findByIds(List.of("t1", "t2"))).thenReturn(List.of(
                new TopicDto("t1", "icebreaker", "text1", List.of("tag1", "tag2")),
                new TopicDto("t2", "drink", "text2", List.of("tag3"))
        ));

        Photo photo = new Photo();
        photo.setS3Key("uuid/photo.jpg");
        when(photoRepository.findByUserIdOrderByPosition(candidateId)).thenReturn(List.of(photo));
        when(photoStorageService.toUrl("uuid/photo.jpg")).thenReturn("/photos/uuid/photo.jpg");

        List<DiscoveryCardDto> cards = discoveryService.discover(currentUserId, venueId, date);

        assertEquals(1, cards.size());
        DiscoveryCardDto card = cards.get(0);
        assertEquals(candidateId, card.userId());
        assertEquals("/photos/uuid/photo.jpg", card.photoUrl());
        assertEquals(List.of("tag1", "tag2", "tag3"), card.topicTags());
        assertEquals("Bar Y", card.venueName());
        assertEquals("ул. Тверская, 5", card.venueAddress());
    }

    @Test
    void discover_noUsersInGeo_returnsEmpty() {
        UUID venueId = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setLatitude(55.76);
        venue.setLongitude(37.64);
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(redisTemplate.opsForGeo()).thenReturn(geoOps);
        when(geoOps.radius(anyString(), any(Circle.class))).thenReturn(null);

        List<DiscoveryCardDto> cards = discoveryService.discover(UUID.randomUUID(), venueId, LocalDate.now());

        assertTrue(cards.isEmpty());
    }
}
