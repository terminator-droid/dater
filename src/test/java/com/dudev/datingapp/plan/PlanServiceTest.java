package com.dudev.datingapp.plan;

import com.dudev.datingapp.common.exception.ConflictException;
import com.dudev.datingapp.common.exception.ResourceNotFoundException;
import com.dudev.datingapp.plan.dto.CreatePlanDto;
import com.dudev.datingapp.plan.dto.PlanDto;
import com.dudev.datingapp.plan.entity.EveningPlan;
import com.dudev.datingapp.plan.entity.PlanStatus;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import com.dudev.datingapp.plan.service.PlanService;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.UserRepository;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.entity.VenueCategory;
import com.dudev.datingapp.venue.repository.VenueRepository;
import com.dudev.datingapp.venue.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock private EveningPlanRepository planRepository;
    @Mock private UserRepository userRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private VenueService venueService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private GeoOperations<String, String> geoOps;
    @Mock private ZSetOperations<String, String> zSetOps;

    private PlanService planService;

    private static final LocalDate TODAY = LocalDate.now();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID VENUE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        planService = new PlanService(planRepository, userRepository, venueRepository, venueService, redisTemplate);
    }

    @Test
    void createPlan_savesPlanAndRegistersInGeo() {
        when(planRepository.existsByUserIdAndVenueIdAndDate(USER_ID, VENUE_ID, TODAY)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));
        Venue venue = buildVenue();
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue));
        when(planRepository.save(any())).thenAnswer(inv -> {
            EveningPlan p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
        when(redisTemplate.opsForGeo()).thenReturn(geoOps);
        when(redisTemplate.expire(anyString(), any())).thenReturn(true);
        when(venueService.toDto(any())).thenCallRealMethod();

        PlanDto result = planService.createPlan(USER_ID, createPlanDto());

        assertThat(result.drinkTonight()).isEqualTo("негрони");
        assertThat(result.status()).isEqualTo(PlanStatus.PLANNED);
        assertThat(result.topicIds()).containsExactly("t1", "t2");
        verify(geoOps).add(eq(PlanService.GEO_KEY_PREFIX + TODAY), any(), eq(USER_ID.toString()));
    }

    @Test
    void createPlan_throwsConflictOnDuplicate() {
        when(planRepository.existsByUserIdAndVenueIdAndDate(USER_ID, VENUE_ID, TODAY)).thenReturn(true);

        assertThatThrownBy(() -> planService.createPlan(USER_ID, createPlanDto()))
                .isInstanceOf(ConflictException.class);

        verify(planRepository, never()).save(any());
    }

    @Test
    void getPlans_returnsUserPlansForDate() {
        EveningPlan plan = buildPlan();
        when(planRepository.findByUserIdAndDate(USER_ID, TODAY)).thenReturn(List.of(plan));
        when(venueService.toDto(any())).thenCallRealMethod();

        List<PlanDto> result = planService.getPlans(USER_ID, TODAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).drinkTonight()).isEqualTo("вино");
    }

    @Test
    void deletePlan_removesPlanAndGeoEntry() {
        UUID planId = UUID.randomUUID();
        EveningPlan plan = buildPlan();
        ReflectionTestUtils.setField(plan, "id", planId);
        when(planRepository.findByIdAndUserId(planId, USER_ID)).thenReturn(Optional.of(plan));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        planService.deletePlan(USER_ID, planId);

        verify(planRepository).delete(plan);
        verify(zSetOps).remove(PlanService.GEO_KEY_PREFIX + TODAY, USER_ID.toString());
    }

    @Test
    void deletePlan_throwsNotFoundForOtherUserPlan() {
        UUID planId = UUID.randomUUID();
        when(planRepository.findByIdAndUserId(planId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.deletePlan(USER_ID, planId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreatePlanDto createPlanDto() {
        return new CreatePlanDto(VENUE_ID, TODAY, "негрони", List.of("t1", "t2"));
    }

    private EveningPlan buildPlan() {
        EveningPlan plan = new EveningPlan();
        plan.setUser(buildUser());
        plan.setVenue(buildVenue());
        plan.setDate(TODAY);
        plan.setDrinkTonight("вино");
        plan.setTopicIds(List.of("t1"));
        return plan;
    }

    private User buildUser() {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", USER_ID);
        u.setPhone("+79001234567");
        u.setPassword("hashed");
        return u;
    }

    private Venue buildVenue() {
        Venue v = new Venue();
        ReflectionTestUtils.setField(v, "id", VENUE_ID);
        v.setName("32.05");
        v.setAddress("Малая Бронная ул., 32");
        v.setLatitude(55.7644);
        v.setLongitude(37.5931);
        v.setArea("Патриаршие");
        v.setCategory(VenueCategory.BAR);
        return v;
    }
}
